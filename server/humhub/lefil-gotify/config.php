<?php
declare(strict_types=1);

use humhub\modules\lefilgotify\Events;

return [
    'id' => 'lefil-gotify',
    'class' => 'humhub\modules\lefilgotify\Module',
    'namespace' => 'humhub\modules\lefilgotify',
    'events' => [
        // HumHub instantiates a module class lazily; registering the target here
        // guarantees it exists for web requests and for console queue workers.
        ['class' => \yii\base\Application::class, 'event' => \yii\base\Application::EVENT_BEFORE_REQUEST, 'callback' => [Events::class, 'onBeforeRequest']],
        // Messenger (Talk) does not use HumHub notification targets for new messages.
        ['class' => 'humhub\modules\mail\models\MessageEntry', 'event' => \yii\db\BaseActiveRecord::EVENT_AFTER_INSERT, 'callback' => [Events::class, 'onMessageEntryInsert']],
        ['class' => \yii\web\User::class, 'event' => \yii\web\User::EVENT_BEFORE_LOGOUT, 'callback' => [Events::class, 'onBeforeLogout']],
    ],
];
