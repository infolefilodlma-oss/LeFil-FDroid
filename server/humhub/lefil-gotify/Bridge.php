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
    public static function configured(): bool
    {
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

    public static function deliver(BaseNotification $notification, User $recipient): bool
    {
        $record = $notification->record;
        if (!$record || (int)$record->user_id !== (int)$recipient->id || (int)$record->id < 1) {
            return false;
        }
        $source = $notification->moduleId === 'mail' ? 'talk' : 'atrium';
        $url = $source === 'talk'
            ? 'https://atrium.le-fil.net/index.php?r=mail%2Fmail%2Findex'
            : 'https://atrium.le-fil.net/index.php?r=notification%2Fentry&id=' . (int)$record->id;
        return self::broker()->deliver((int)$recipient->id, [
            'title' => 'Le Fil',
            'message' => $source === 'talk' ? 'Nouveau message dans Talk' : 'Nouvelle notification Atrium',
            'priority' => 5,
            'extras' => [
                'lefil::notification' => [
                    'source' => $source,
                    'event_id' => 'humhub:' . (int)$record->id,
                    'url' => $url,
                ],
            ],
        ]);
    }
}
