<?php
declare(strict_types=1);
/**
 * Native Friendica fixture for the Le Fil Agora addon, run on a disposable CI database only.
 * Usage (from the Friendica root): php FriendicaFixture.php enroll|event|verify|logout
 */
use Friendica\DI;

require getcwd() . '/vendor/autoload.php';
$container = \Friendica\Core\DiceContainer::fromBasePath(getcwd());
$app = \Friendica\App::fromContainer($container);
// Same bootstrap as Friendica's own console (App::processConsole), without a console command.
(\Closure::bind(function (array $server) {
    $this->setupContainerForAddons();
    $this->setupLegacyServiceLocator();
    $this->registerEventDispatcher();
    $this->load(
        $server,
        $this->container->create(\Friendica\Database\Definition\DbaDefinition::class),
        $this->container->create(\Friendica\Database\Definition\ViewDefinition::class),
        $this->container->create(\Friendica\App\Mode::class),
        $this->container->create(\Friendica\Core\Config\Capability\IManageConfigValues::class),
        $this->container->create(\Friendica\Util\Profiler::class),
        $this->container->create(\Psr\EventDispatcher\EventDispatcherInterface::class),
        $this->container->create(\Friendica\AppHelper::class),
        $this->container->create(\Friendica\Core\Addon\AddonHelper::class),
    );
    $this->registerTemplateEngine();
}, $app, \Friendica\App::class))($_SERVER);

require_once getcwd() . '/addon/lefilgotify/lefilgotify.php';

$statePath = getenv('LEFIL_FIXTURE_FILE');
$state = is_file($statePath) ? json_decode(file_get_contents($statePath), true) : [];
$store = static function (array $value) use ($statePath): void { file_put_contents($statePath, json_encode($value)); chmod($statePath, 0600); };
$uid = static function (string $nick): int {
    $user = \Friendica\Model\User::getByNickname($nick, ['uid']);
    if (empty($user['uid'])) throw new RuntimeException('Unknown fixture member ' . $nick);
    return (int)$user['uid'];
};
$gotify = static function (string $token): array {
    $curl = curl_init('http://127.0.0.1:18765/message?limit=100');
    curl_setopt_array($curl, [CURLOPT_RETURNTRANSFER => true, CURLOPT_HTTPHEADER => ['X-Gotify-Key: ' . $token], CURLOPT_TIMEOUT => 5, CURLOPT_PROXY => '']);
    $json = curl_exec($curl); $status = (int)curl_getinfo($curl, CURLINFO_RESPONSE_CODE); curl_close($curl);
    return [$status, $status === 200 ? json_decode($json, true, 64, JSON_THROW_ON_ERROR) : null];
};

switch ($argv[1] ?? '') {
    case 'enroll':
        if (!lefilgotify_configured()) throw new RuntimeException('Addon not configured');
        foreach (['a', 'b'] as $name) {
            $credentials = lefilgotify_broker()->provision($uid('lefil_' . $name), hash('sha256', 'agora-device-' . $name), 'agora-session-' . $name);
            if (($credentials['server'] ?? '') !== 'https://notifications.le-fil.net') throw new RuntimeException('Enrollment contract broken');
            $state['friendica'][$name] = $credentials['client_token'];
        }
        $store($state);
        echo "Agora members paired through the Le Fil addon.\n";
        break;
    case 'event':
        // Friendica's own notification path for a private message: Notify::createFromArray -> 'enotify' hook.
        DI::notify()->createFromArray([
            'type' => \Friendica\Model\Notification\Type::MAIL,
            'otype' => \Friendica\Model\Notification\ObjectType::MAIL,
            'verb' => \Friendica\Protocol\Activity::POST,
            'uid' => $uid('lefil_a'),
            'source_name' => 'Fixture B',
            'source_link' => 'https://agora.le-fil.net/profile/lefil_b',
            'link' => 'https://agora.le-fil.net/message/4242',
            'item' => ['body' => 'PRIVATE_BODY_SENTINEL', 'title' => 'PRIVATE_BODY_SENTINEL'],
        ]);
        echo "Native Agora private message notification processed.\n";
        break;
    case 'verify':
        [$status, $page] = $gotify($state['friendica']['a']);
        if ($status !== 200) throw new RuntimeException('Recipient history unreadable');
        $messages = $page['messages'];
        if (count($messages) !== 1) throw new RuntimeException('Expected one Agora notification, got ' . count($messages));
        if (str_contains(json_encode($messages), 'PRIVATE_BODY_SENTINEL')) throw new RuntimeException('Private body leaked');
        $n = $messages[0]['extras']['lefil::notification'] ?? [];
        if (($n['source'] ?? '') !== 'agora' || ($n['url'] ?? '') !== 'https://agora.le-fil.net/message/4242' || !preg_match('/^f[0-9a-f]{40}$/D', $n['event_id'] ?? '')) {
            throw new RuntimeException('Android contract mismatch: ' . json_encode($n));
        }
        [$status, $other] = $gotify($state['friendica']['b']);
        if ($status !== 200 || count($other['messages']) !== 0) throw new RuntimeException('Recipient privacy isolation failed');
        echo "Agora private message delivered natively to the recipient only, without private text.\n";
        break;
    case 'logout':
        lefilgotify_broker()->logout($uid('lefil_a'), 'agora-session-a');
        if ($gotify($state['friendica']['a'])[0] !== 401) throw new RuntimeException('Logout did not revoke the device token');
        if ($gotify($state['friendica']['b'])[0] !== 200) throw new RuntimeException('Logout of one member affected another');
        echo "Agora logout revoked only the member's own device.\n";
        break;
    default:
        throw new RuntimeException('Unknown fixture action');
}
