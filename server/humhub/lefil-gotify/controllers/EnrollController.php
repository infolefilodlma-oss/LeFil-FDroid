<?php
declare(strict_types=1);

namespace humhub\modules\lefilgotify\controllers;

use humhub\components\access\ControllerAccess;
use humhub\components\Controller;
use humhub\modules\lefilgotify\Bridge;
use Yii;
use yii\web\Response;

/**
 * Device enrollment for the Le Fil Android app, bound to the member's own HumHub session.
 * CSRF tokens are not used because the app is not a web page; instead a custom request
 * header is mandatory, which a foreign web page cannot send cross-site without a CORS
 * preflight that this endpoint never grants.
 */
final class EnrollController extends Controller
{
    public $enableCsrfValidation = false;

    protected function getAccessRules()
    {
        return [[ControllerAccess::RULE_LOGGED_IN_ONLY], [ControllerAccess::RULE_POST => ['index', 'revoke']]];
    }

    public function actionIndex()
    {
        return $this->guarded(static fn(int $uid, string $device, string $session): array =>
            Bridge::provision($uid, $device, $session));
    }

    public function actionRevoke()
    {
        return $this->guarded(static function (int $uid, string $device): array {
            Bridge::revoke($uid, $device);
            return ['revoked' => true];
        });
    }

    private function guarded(callable $action): Response
    {
        $request = Yii::$app->request;
        $response = Yii::$app->response;
        $response->headers->set('Cache-Control', 'no-store');
        if ($request->headers->get('X-LeFil-App') !== 'enroll' || !Bridge::configured()) {
            $response->statusCode = 403;
            return $this->asJson(['error' => 'unavailable']);
        }
        $device = (string)$request->post('device', '');
        if (!preg_match('/^[a-f0-9]{64}$/D', $device)) {
            $response->statusCode = 400;
            return $this->asJson(['error' => 'invalid']);
        }
        try {
            return $this->asJson($action((int)Yii::$app->user->id, $device, (string)Yii::$app->session->id));
        } catch (\Throwable $e) {
            Yii::warning('Le Fil device enrollment unavailable', 'lefil-gotify');
            $response->statusCode = 503;
            return $this->asJson(['error' => 'unavailable']);
        }
    }
}
