<?php
declare(strict_types=1);

namespace humhub\modules\lefilgotify\targets;

use humhub\modules\lefilgotify\Bridge;
use humhub\modules\notification\components\BaseNotification;
use humhub\modules\notification\targets\BaseTarget;
use humhub\modules\user\models\User;
use Yii;

/** Additive, opt-in delivery target: a missing configuration must not block HumHub notifications. */
final class GotifyTarget extends BaseTarget
{
    public $id = 'lefil-gotify';
    public $defaultSetting = true;

    public function getTitle()
    {
        return 'Le Fil';
    }

    public function isActive(?User $user = null)
    {
        return parent::isActive($user) && $user !== null && Bridge::configured();
    }

    public function handle(BaseNotification $notification, User $user)
    {
        try {
            Bridge::deliver($notification, $user);
        } catch (\Throwable $e) {
            // Never reveal credentials or personal content in exception logs.
            Yii::warning('Le Fil native notification delivery unavailable', 'lefil-gotify');
        }
    }
}
