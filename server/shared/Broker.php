<?php
declare(strict_types=1);
namespace LeFil\Notifications;

/** Native framework adapters provide current user ID and native per-user settings.
 * Credentials are sealed before persistence; no credential is embedded in this code.
 */
final class Broker
{
    private string $key;
    public function __construct(private string $realm, private \Closure $load, private \Closure $save)
    {
        if (!in_array($realm, ['humhub', 'friendica'], true)) throw new \RuntimeException('Invalid realm');
        $path = getenv('LEFIL_GOTIFY_KEY_FILE');
        $key = $path ? @file_get_contents($path) : false;
        if ($key === false || strlen($key) !== SODIUM_CRYPTO_SECRETBOX_KEYBYTES) throw new \RuntimeException('Gotify enrollment unavailable');
        $this->key = $key;
    }

    private function subject(int $uid): string
    {
        if ($uid < 1) throw new \RuntimeException('Authentication required');
        return $this->realm . '-' . substr(hash_hmac('sha256', (string)$uid, $this->key), 0, 40);
    }

    public function identity(int $uid): string { return $this->subject($uid); }

    private function api(string $method, string $path, array $auth, ?array $body = null): array
    {
        if (!preg_match('~^/(?:user|application|client|message)(?:/[0-9]+)?(?:\\?limit=100&since=[0-9]+)?$~D', $path)) throw new \RuntimeException('Invalid internal request');
        $curl = curl_init('http://127.0.0.1:18765' . $path);
        $headers = ['Content-Type: application/json'];
        $options = [CURLOPT_CUSTOMREQUEST => $method, CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => false, CURLOPT_CONNECTTIMEOUT_MS => 1500, CURLOPT_TIMEOUT_MS => 8000,
            CURLOPT_PROXY => '', CURLOPT_VERBOSE => false];
        if (isset($auth['token'])) {
            if (!preg_match('/^[A-Za-z0-9_.-]{16,512}$/D', $auth['token'])) throw new \RuntimeException('Invalid credential');
            $headers[] = 'X-Gotify-Key: ' . $auth['token'];
        } else {
            $options[CURLOPT_USERPWD] = $auth['name'] . ':' . $auth['password'];
        }
        if ($body !== null) $options[CURLOPT_POSTFIELDS] = json_encode($body, JSON_THROW_ON_ERROR);
        $options[CURLOPT_HTTPHEADER] = $headers;
        curl_setopt_array($curl, $options);
        try {
            $data = curl_exec($curl);
            $status = (int)curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
            if ($data === false || $status < 200 || $status >= 300) throw new \RuntimeException(sprintf('Gotify HTTP failure: %s %s status=%d', $method, $path, $status));
            return $data === '' ? [] : json_decode($data, true, 64, JSON_THROW_ON_ERROR);
        } finally { curl_close($curl); }
    }

    private function admin(): array
    {
        $path = getenv('LEFIL_GOTIFY_ADMIN_PASSWORD_FILE');
        $password = $path ? @file_get_contents($path) : false;
        if ($password === false || strlen(trim($password)) < 24) throw new \RuntimeException('Gotify enrollment unavailable');
        return ['name' => 'admin', 'password' => trim($password)];
    }

    private function read(int $uid): array
    {
        $sealed = ($this->load)($uid);
        if (!$sealed) return [];
        $raw = base64_decode($sealed, true);
        if ($raw === false || strlen($raw) <= SODIUM_CRYPTO_SECRETBOX_NONCEBYTES) throw new \RuntimeException('Invalid private state');
        $plain = sodium_crypto_secretbox_open(substr($raw, SODIUM_CRYPTO_SECRETBOX_NONCEBYTES), substr($raw, 0, SODIUM_CRYPTO_SECRETBOX_NONCEBYTES), $this->key);
        if ($plain === false) throw new \RuntimeException('Invalid private state');
        $state = json_decode($plain, true, 64, JSON_THROW_ON_ERROR);
        if (($state['subject'] ?? '') !== $this->subject($uid)) throw new \RuntimeException('Private state ownership mismatch');
        return $state;
    }

    private function write(int $uid, array $state): void
    {
        $nonce = random_bytes(SODIUM_CRYPTO_SECRETBOX_NONCEBYTES);
        ($this->save)($uid, base64_encode($nonce . sodium_crypto_secretbox(json_encode($state, JSON_THROW_ON_ERROR), $nonce, $this->key)));
    }

    private function locked(int $uid, callable $action): mixed
    {
        $directory = getenv('LEFIL_GOTIFY_LOCK_DIR');
        if (!$directory || !is_dir($directory) || !is_writable($directory)) throw new \RuntimeException('Private lock directory required');
        $file = $directory . '/' . $this->subject($uid) . '.lock';
        $handle = fopen($file, 'c');
        if (!$handle) throw new \RuntimeException('Enrollment lock unavailable');
        chmod($file, 0600);
        try {
            if (!flock($handle, LOCK_EX)) throw new \RuntimeException('Enrollment lock unavailable');
            return $action();
        } finally { flock($handle, LOCK_UN); fclose($handle); }
    }

    private function account(int $uid): array
    {
        $state = $this->read($uid);
        if ($state) return $state;
        $name = 'lefil-' . $this->subject($uid);
        // Recoverable, strong per-account password derived from a server-only key.
        $password = hash_hmac('sha512', 'gotify-password:' . $name, $this->key);
        $user = null;
        foreach ($this->api('GET', '/user', $this->admin()) as $candidate) {
            if (($candidate['name'] ?? '') === $name) { $user = $candidate; break; }
        }
        if (!$user) $user = $this->api('POST', '/user', $this->admin(), ['name' => $name, 'pass' => $password, 'admin' => false]);
        $auth = ['name' => $name, 'password' => $password];
        $app = $this->api('POST', '/application', $auth, ['name' => 'Le Fil', 'description' => 'Private native notifications']);
        $state = ['subject' => $this->subject($uid), 'user_id' => (int)$user['id'], 'name' => $name,
            'password' => $password, 'application_token' => $app['token'], 'devices' => []];
        $this->write($uid, $state);
        return $state;
    }

    private function device(int $uid, string $device): string
    {
        if (!preg_match('/^[a-f0-9]{64}$/D', $device)) throw new \RuntimeException('Invalid device');
        return hash_hmac('sha256', $this->subject($uid) . ':' . $device, $this->key);
    }

    private function session(string $session): string
    {
        if ($session === '') throw new \RuntimeException('Authenticated session required');
        return hash_hmac('sha256', 'session:' . $session, $this->key);
    }

    public function provision(int $uid, string $device, string $session): array
    {
        return $this->locked($uid, function () use ($uid, $device, $session) {
            $key = $this->device($uid, $device);
            $sessionTag = $this->session($session);
            $state = $this->account($uid);
            $existing = $state['devices'][$key] ?? null;
            if ($existing && hash_equals($existing['session'], $sessionTag)) {
                return ['subject' => $state['subject'], 'server' => 'https://notifications.le-fil.net', 'client_token' => $existing['token']];
            }
            $auth = ['name' => $state['name'], 'password' => $state['password']];
            if ($existing) {
                $this->api('DELETE', '/client/' . $existing['id'], $auth);
                unset($state['devices'][$key]);
                $this->write($uid, $state);
            }
            if (count($state['devices']) >= 8) throw new \RuntimeException('Device limit reached');
            $client = $this->api('POST', '/client', $auth, ['name' => 'Le Fil ' . substr($key, 0, 12)]);
            $state['devices'][$key] = ['id' => (int)$client['id'], 'token' => $client['token'], 'session' => $sessionTag];
            $this->write($uid, $state);
            return ['subject' => $state['subject'], 'server' => 'https://notifications.le-fil.net', 'client_token' => $client['token']];
        });
    }

    public function revoke(int $uid, string $device): void
    {
        $this->locked($uid, function () use ($uid, $device) {
            $state = $this->read($uid);
            $key = $this->device($uid, $device);
            if (!isset($state['devices'][$key])) return;
            $this->api('DELETE', '/client/' . $state['devices'][$key]['id'], ['name' => $state['name'], 'password' => $state['password']]);
            unset($state['devices'][$key]);
            $this->write($uid, $state);
        });
    }

    public function logout(int $uid, string $session): void
    {
        $this->locked($uid, function () use ($uid, $session) {
            $state = $this->read($uid);
            if (!$state) return;
            $tag = $this->session($session);
            foreach ($state['devices'] as $key => $device) {
                if (!hash_equals($tag, $device['session'])) continue;
                $this->api('DELETE', '/client/' . $device['id'], ['name' => $state['name'], 'password' => $state['password']]);
                unset($state['devices'][$key]);
                $this->write($uid, $state);
            }
        });
    }

    public function history(int $uid, string $device, string $session, string $token, int $since): array
    {
        $state = $this->read($uid);
        $binding = $state['devices'][$this->device($uid, $device)] ?? null;
        if (!$binding || !hash_equals($binding['session'], $this->session($session)) || !hash_equals($binding['token'], $token)) throw new \RuntimeException('Device authentication required');
        if ($since < 0) throw new \RuntimeException('Invalid history cursor');
        $page = $this->api('GET', '/message?limit=100&since=' . $since, ['token' => $token]);
        // Return only numeric paging. Never follow a remotely supplied URL.
        return ['messages' => $page['messages'], 'next_since' => (int)($page['paging']['since'] ?? 0)];
    }

    public function deliver(int $uid, array $payload): bool
    {
        $state = $this->read($uid);
        if (!$state || !$state['devices']) return false;
        $this->api('POST', '/message', ['token' => $state['application_token']], $payload);
        return true;
    }
}
