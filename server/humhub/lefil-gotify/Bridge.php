<?php
declare(strict_types=1);

namespace humhub\modules\lefilgotify;

use humhub\modules\notification\components\BaseNotification;
use humhub\modules\user\models\User;
use LeFil\Notifications\Broker;
use Yii;

/** Only the authenticated HumHub user's own settings may hold their encrypted Gotify state. */
final class Bridge
{
    /** Server-only settings file, readable by the HumHub system user, never inside the web root. */
    private const SETTINGS_FILE = '/etc/lefil-gotify/settings.php';
    private const ATRIUM = 'https://atrium.le-fil.net';

    private static bool $settingsLoaded = false;

    /** PHP-FPM pools may clear the environment; fall back to the administrator's settings file. */
    private static function loadSettings(): void
    {
        if (self::$settingsLoaded) {
            return;
        }
        self::$settingsLoaded = true;
        if (getenv('LEFIL_GOTIFY_KEY_FILE') || !@is_file(self::SETTINGS_FILE) || is_link(self::SETTINGS_FILE)) {
            return;
        }
        $values = include self::SETTINGS_FILE;
        if (!is_array($values)) {
            return;
        }
        foreach ($values as $name => $value) {
            if (is_string($name) && preg_match('/^LEFIL_GOTIFY_[A-Z_]+$/D', $name) && is_string($value)) {
                putenv($name . '=' . $value);
            }
        }
    }

    public static function configured(): bool
    {
        self::loadSettings();
        foreach (['LEFIL_GOTIFY_KEY_FILE', 'LEFIL_GOTIFY_LOCK_DIR', 'LEFIL_GOTIFY_BROKER_FILE'] as $name) {
            $path = getenv($name);
            if (!$path || ($name === 'LEFIL_GOTIFY_LOCK_DIR' ? !is_dir($path) : !is_file($path))) {
                return false;
            }
        }
        return true;
    }

    private static function broker(): Broker
    {
        if (!self::configured()) {
            throw new \RuntimeException('Le Fil notification bridge unavailable');
        }
        // Exact administrator-provisioned absolute path, never a web or user-controlled path.
        $file = getenv('LEFIL_GOTIFY_BROKER_FILE');
        if ($file[0] !== '/' || is_link($file)) {
            throw new \RuntimeException('Invalid internal broker location');
        }
        require_once $file;
        $load = static function (int $uid): ?string {
            $member = User::findOne($uid);
            return $member ? Yii::$app->getModule('lefil-gotify')->settings->user($member)->get('encrypted_gotify') : null;
        };
        $save = static function (int $uid, string $value): void {
            $member = User::findOne($uid);
            if (!$member) {
                throw new \RuntimeException('Unknown member');
            }
            Yii::$app->getModule('lefil-gotify')->settings->user($member)->set('encrypted_gotify', $value);
        };
        return new Broker('humhub', $load, $save);
    }

    public static function provision(int $uid, string $device, string $session): array
    {
        return self::broker()->provision($uid, $device, $session);
    }

    public static function revoke(int $uid, string $device): void
    {
        self::broker()->revoke($uid, $device);
    }

    public static function logout(int $uid, string $session): void
    {
        if ($uid > 0 && $session !== '') {
            self::broker()->logout($uid, $session);
        }
    }

    private static function payload(string $source, string $eventId, string $url): array
    {
        return [
            'title' => 'Le Fil',
            // Never the private body: only a fixed label leaves HumHub.
            'message' => $source === 'talk' ? 'Nouveau message dans Talk' : 'Nouvelle notification Atrium',
            'priority' => 5,
            'extras' => ['lefil::notification' => ['source' => $source, 'event_id' => $eventId, 'url' => $url]],
        ];
    }

    /** Atrium activity, through HumHub's own notification targets. */
    public static function deliver(BaseNotification $notification, User $recipient): bool
    {
        $record = $notification->record;
        // Messenger new messages are delivered by deliverTalk(); never twice.
        if ($notification->moduleId === 'mail' || !$record || (int)$record->user_id !== (int)$recipient->id || (int)$record->id < 1) {
            return false;
        }
        return self::broker()->deliver((int)$recipient->id, self::payload(
            'atrium',
            'humhub:' . (int)$record->id,
            self::ATRIUM . '/index.php?r=notification%2Fentry&id=' . (int)$record->id
        ));
    }

    /** Talk (HumHub Messenger): one native notification per recipient, never to the author. */
    public static function deliverTalk($entry): int
    {
        // AbstractMessageEntry::TYPE_MESSAGE; joined/left markers are not messages.
        if (!is_object($entry) || (int)$entry->type !== 0 || (int)$entry->id < 1) {
            return 0;
        }
        $message = $entry->message;
        if (!$message || (int)$message->id < 1) {
            return 0;
        }
        $sent = 0;
        foreach ($message->users as $member) {
            if ((int)$member->id === (int)$entry->user_id || (int)$member->status !== User::STATUS_ENABLED) {
                continue;
            }
            try {
                if (self::broker()->deliver((int)$member->id, self::payload(
                    'talk',
                    'talk:' . (int)$entry->id,
                    self::ATRIUM . '/index.php?r=mail%2Fmail%2Findex&id=' . (int)$message->id
                ))) {
                    $sent++;
                }
            } catch (\Throwable $e) {
                Yii::warning('Le Fil native Talk delivery unavailable for one recipient', 'lefil-gotify');
            }
        }
        return $sent;
    }
}
