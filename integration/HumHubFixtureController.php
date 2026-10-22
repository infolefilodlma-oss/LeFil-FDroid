<?php
namespace humhub\commands;
use humhub\modules\user\models\User;
use humhub\modules\user\models\Password;
use humhub\modules\lefilgotify\Bridge;
use humhub\modules\mail\models\forms\CreateMessage;
use humhub\modules\user\notifications\Followed;
use Yii;
class LeFilFixtureController extends \yii\console\Controller {
 private function state():array {return json_decode(file_get_contents(getenv('LEFIL_FIXTURE_FILE')),true);}
 private function store(array $value):void {$path=getenv('LEFIL_FIXTURE_FILE');file_put_contents($path,json_encode($value));chmod($path,0600);}
 public function actionSeed() {
  foreach(['mail','lefil-gotify'] as $id) Yii::$app->moduleManager->enable(Yii::$app->moduleManager->getModule($id));
  \humhub\services\MigrationService::create()->migrateUp();
  $state=is_file(getenv('LEFIL_FIXTURE_FILE'))?$this->state():[];
  foreach(['sender','a','b'] as $name) {
   $user=new User(['username'=>'fixture_'.$name,'email'=>'hh_'.$name.'@example.com','status'=>User::STATUS_ENABLED]);
   if (!$user->save()) throw new \RuntimeException('Native HumHub user invalid fields: '.implode(',',array_keys($user->getErrors())));
   // Minimal native account fixture: no optional profile schema assumptions.
   $password=bin2hex(random_bytes(24));
   $model=new Password(['user_id'=>$user->id]);$model->setPassword($password);
   if (!$model->save()) throw new \RuntimeException('Native password persistence failed');
   $state['humhub'][$name]=['id'=>(int)$user->id,'username'=>$user->username,'password'=>$password];
  }
  $sender=User::findOne($state['humhub']['sender']['id']);
  \humhub\modules\user\models\Group::getAdminGroup()->addUser($sender);
  $this->store($state);
  echo "HumHub native fixtures created.\n";
 }
 public function actionEvents() {
  $state=$this->state()['humhub'];$sender=User::findOne($state['sender']['id']);$a=User::findOne($state['a']['id']);
  Yii::$app->user->switchIdentity($sender);
  Followed::instance()->from($sender)->about($a)->send($a);
  $form=new CreateMessage;
  $form->recipient = [$a->guid];$form->title='Fixture';$form->message='PRIVATE_BODY_SENTINEL';
  if (!$form->save()) throw new \RuntimeException('Native Talk send failed');
  // Process the actual database queue only after the native send transaction committed.
  Yii::$app->queue->run(false);
  echo "Native Atrium and Talk events processed.\n";
 }
 public function actionTransactionalRollback() {
  $state=$this->state()['humhub'];$sender=User::findOne($state['sender']['id']);$a=User::findOne($state['a']['id']);
  Yii::$app->user->switchIdentity($sender);
  $tx=Yii::$app->db->beginTransaction();
  Followed::instance()->from($sender)->about($a)->send($a);
  $tx->rollBack();
  Yii::$app->queue->run(false);
  echo "Native rollback processed.\n";
 }
}
