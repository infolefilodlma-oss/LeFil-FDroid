<?php
declare(strict_types=1);

namespace humhub\modules\lefilgotify;

use humhub\modules\lefilgotify\targets\GotifyTarget;
use Yii;

/**
 * Adds an independent native target; never replaces HumHub's existing mobile/FCM target.
 * Without server-only credentials, this target stays inactive.
 */
final class Module extends \humhub\components\Module
{
    public function init()
    {
        parent::init();
        $notification = Yii::$app->get('notification', false);
        if ($notification !== null && !isset($notification->targets[GotifyTarget::class])) {
            $notification->targets[GotifyTarget::class] = [];
        }
    }

    public function getNotifications()
    {
        return [];
    }
}
