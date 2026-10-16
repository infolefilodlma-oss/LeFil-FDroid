<?php

namespace humhub\modules\messengercontacts\controllers;

use humhub\components\Controller;
use humhub\components\access\ControllerAccess;
use humhub\modules\mail\helpers\Url;
use humhub\modules\mail\models\MessageEntry;
use humhub\modules\mail\models\UserMessage;
use Yii;

/**
 * PROJET NON DEPLOYE : lecture des evenements de la messagerie Talk du
 * destinataire authentifie, sans contenu des messages.
 *
 * Premier appel (after=0) : les 100 derniers evenements servent de point
 * de depart, l'application n'emet aucune notification sur cet historique.
 * Appels suivants : parcours ASCENDANT depuis l'ID confirme par Android,
 * par lots de 100 pour ne pas perdre les messages lors d'une longue pause.
 */
class LeFilNotificationController extends Controller
{
    protected function getAccessRules()
    {
        return [
            [ControllerAccess::RULE_LOGGED_IN_ONLY],
            [ControllerAccess::RULE_DENY_IMPERSONATED],
        ];
    }

    public function actionIndex()
    {
        $user = Yii::$app->user->getIdentity();
        if ($user === null) {
            Yii::$app->response->statusCode = 401;
            return $this->asJson(['recipient' => null, 'events' => []]);
        }

        $rawAfter = Yii::$app->request->get('after', '0');
        $after = filter_var($rawAfter, FILTER_VALIDATE_INT, [
            'options' => ['min_range' => 0],
        ]);
        if ($after === false) {
            Yii::$app->response->statusCode = 400;
            return $this->asJson(['recipient' => null, 'events' => []]);
        }

        $bootstrap = ($after === 0);
        $entries = MessageEntry::find()
            ->alias('me')
            ->innerJoin(
                ['um' => UserMessage::tableName()],
                'um.message_id = me.message_id'
            )
            ->where(['um.user_id' => $user->id])
            ->andWhere(['>', 'me.id', $after])
            ->andWhere(['me.type' => MessageEntry::TYPE_MESSAGE])
            ->andWhere(['<>', 'me.created_by', $user->id])
            ->orderBy(['me.id' => $bootstrap ? SORT_DESC : SORT_ASC])
            ->limit(100)
            ->with('message')
            ->all();

        if ($bootstrap) {
            $entries = array_reverse($entries);
        }

        $events = [];
        $isParticipant = [];
        foreach ($entries as $entry) {
            $message = $entry->message;
            if ($message === null) {
                continue;
            }

            $conversationId = (int) $message->id;
            if (!array_key_exists($conversationId, $isParticipant)) {
                $isParticipant[$conversationId] = $message->isParticipant($user);
            }
            if (!$isParticipant[$conversationId]) {
                continue;
            }

            $events[] = [
                'source' => 'atrium',
                'id' => (int) $entry->id,
                'conversation_id' => $conversationId,
                'url' => Url::toMessenger($message, true),
            ];
        }

        return $this->asJson([
            'recipient' => (string) $user->guid,
            'events' => $events,
        ]);
    }
}
