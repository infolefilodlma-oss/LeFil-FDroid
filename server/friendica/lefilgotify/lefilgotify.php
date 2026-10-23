<?php
/**
 * Name: Le Fil native notifications
 * Description: Private delivery of Agora notifications to the Le Fil Android app (Gotify, no Google service). Inactive until configured by the server administrator.
 * Version: 0.1
 * Author: Le Fil
 */
declare(strict_types=1);

use Friendica\Core\Hook;
use Friendica\Core\System;
use Friendica\DI;

const LEFILGOTIFY_SETTINGS_FILE = '/etc/lefil-gotify/settings.php';
const LEFILGOTIFY_AGORA = 'https://agora.le-fil.net';

function lefilgotify_install(): void
{
	Hook::register('enotify', __FILE__, 'lefilgotify_enotify');
	Hook::register('logging_out', __FILE__, 'lefilgotify_logging_out');
}

/** PHP-FPM pools may clear the environment; fall back to the administrator's settings file. */
function lefilgotify_configured(): bool
{
	static $loaded = false;
	if (!$loaded) {
		$loaded = true;
		if (!getenv('LEFIL_GOTIFY_KEY_FILE') && @is_file(LEFILGOTIFY_SETTINGS_FILE) && !is_link(LEFILGOTIFY_SETTINGS_FILE)) {
			$values = include LEFILGOTIFY_SETTINGS_FILE;
			foreach (is_array($values) ? $values : [] as $name => $value) {
				if (is_string($name) && preg_match('/^LEFIL_GOTIFY_[A-Z_]+$/D', $name) && is_string($value)) {
					putenv($name . '=' . $value);
				}
			}
		}
	}
	foreach (['LEFIL_GOTIFY_KEY_FILE', 'LEFIL_GOTIFY_LOCK_DIR', 'LEFIL_GOTIFY_BROKER_FILE'] as $name) {
		$path = getenv($name);
		if (!$path || ($name === 'LEFIL_GOTIFY_LOCK_DIR' ? !is_dir($path) : !is_file($path))) {
			return false;
		}
	}
	return true;
}

function lefilgotify_broker(): \LeFil\Notifications\Broker
{
	if (!lefilgotify_configured()) {
		throw new RuntimeException('Le Fil notification bridge unavailable');
	}
	$file = getenv('LEFIL_GOTIFY_BROKER_FILE');
	if ($file[0] !== '/' || is_link($file)) {
		throw new RuntimeException('Invalid internal broker location');
	}
	require_once $file;
	// Encrypted state lives in the member's own Friendica personal configuration.
	$load = static fn(int $uid): ?string => DI::pConfig()->get($uid, 'lefilgotify', 'state') ?: null;
	$save = static function (int $uid, string $value): void {
		DI::pConfig()->set($uid, 'lefilgotify', 'state', $value);
	};
	return new \LeFil\Notifications\Broker('friendica', $load, $save);
}

/** Navigation link for the app: only an Agora HTTPS URL, otherwise the notifications page. */
function lefilgotify_link(string $candidate): string
{
	$parts = parse_url($candidate);
	if (is_array($parts) && ($parts['scheme'] ?? '') === 'https' && ($parts['host'] ?? '') === 'agora.le-fil.net'
		&& !isset($parts['user']) && !isset($parts['pass']) && !isset($parts['port'])) {
		return $candidate;
	}
	return LEFILGOTIFY_AGORA . '/notifications';
}

/** Hook 'enotify': fired by Friendica for every notification of a local member, private messages included. */
function lefilgotify_enotify(array &$b): void
{
	try {
		$params = $b['params'] ?? [];
		$uid = (int)($params['uid'] ?? 0);
		if ($uid < 1 || !lefilgotify_configured()) {
			return;
		}
		$link = (string)(($params['type'] ?? 0) == \Friendica\Model\Notification\Type::MAIL ? ($params['link'] ?? '') : ($b['itemlink'] ?? ''));
		$identity = implode('|', [$uid, (string)($params['type'] ?? ''), $link, (string)($params['item']['uri-id'] ?? ''),
			(string)($params['parent'] ?? ''), (string)($params['verb'] ?? '')]);
		lefilgotify_broker()->deliver($uid, [
			'title' => 'Le Fil',
			// Never the private body: only a fixed label leaves Friendica.
			'message' => 'Nouvelle activité sur Agora',
			'priority' => 5,
			'extras' => ['lefil::notification' => [
				'source' => 'agora',
				'event_id' => 'f' . substr(hash('sha256', $identity), 0, 40),
				'url' => lefilgotify_link($link),
			]],
		]);
	} catch (Throwable $e) {
		DI::logger()->warning('Le Fil native Agora delivery unavailable');
	}
}

function lefilgotify_logging_out(&$b): void
{
	try {
		$uid = (int)DI::userSession()->getLocalUserId();
		$session = (string)session_id();
		if ($uid > 0 && $session !== '' && lefilgotify_configured()) {
			lefilgotify_broker()->logout($uid, $session);
		}
	} catch (Throwable $e) {
		DI::logger()->warning('Le Fil device logout unavailable');
	}
}

/** Declares https://agora.le-fil.net/lefilgotify as an addon module. */
function lefilgotify_module(): void
{
}

/**
 * POST /lefilgotify (enroll) or /lefilgotify/revoke, for the logged-in member only.
 * A custom header is mandatory: a foreign web page cannot send it cross-site.
 */
function lefilgotify_post(): void
{
	header('Cache-Control: no-store');
	$uid = (int)DI::userSession()->getLocalUserId();
	if ($uid < 1) {
		System::jsonError(403, ['error' => 'login']);
	}
	if (($_SERVER['HTTP_X_LEFIL_APP'] ?? '') !== 'enroll' || !lefilgotify_configured()) {
		System::jsonError(403, ['error' => 'unavailable']);
	}
	$device = (string)($_POST['device'] ?? '');
	if (!preg_match('/^[a-f0-9]{64}$/D', $device)) {
		System::jsonError(400, ['error' => 'invalid']);
	}
	try {
		if (DI::args()->get(1) === 'revoke') {
			lefilgotify_broker()->revoke($uid, $device);
			$result = ['revoked' => true];
		} else {
			$result = lefilgotify_broker()->provision($uid, $device, (string)session_id());
		}
	} catch (Throwable $e) {
		DI::logger()->warning('Le Fil device enrollment unavailable');
		System::jsonError(503, ['error' => 'unavailable']);
	}
	// Exits the request; kept outside the try block so the exit is never swallowed.
	System::jsonExit($result);
}

