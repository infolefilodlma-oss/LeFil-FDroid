<?php
declare(strict_types=1);

namespace humhub\modules\lefilgotify;

use humhub\modules\lefilgotify\targets\GotifyTarget;
use Yii;

/** Every handler is additive and silent on failure: HumHub must never be blocked by Le Fil push. */
final class Events
{
    public static function onBeforeRequest(): void
    {
        try {
            if (!Bridge::configured()) {
                return;
            }
            $notification = Yii::$app->get('notification', false);
            if ($notification !== null && !isset($notification->targets[GotifyTarget::class])) {
                $notification->targets[GotifyTarget::class] = [];
            }
        } catch (\Throwable $e) {
            Yii::warning('Le Fil native notification target unavailable', 'lefil-gotify');
        }
    }

    public static function onMessageEntryInsert($event): void
    {
        try {
            if (Bridge::configured()) {
                Bridge::deliverTalk($event->sender);
            }
        } catch (\Throwable $e) {
            Yii::warning('Le Fil native Talk delivery unavailable', 'lefil-gotify');
        }
    }

    public static function onBeforeLogout($event): void
    {
        try {
            $identity = $event->identity ?? null;
            if ($identity !== null && Bridge::configured() && Yii::$app->has('session')) {
                Bridge::logout((int)$identity->id, (string)Yii::$app->session->id);
            }
        } catch (\Throwable $e) {
            Yii::warning('Le Fil device logout unavailable', 'lefil-gotify');
        }
    }
}
