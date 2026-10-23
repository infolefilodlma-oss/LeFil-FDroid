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
 /** Pairs a device for members a and b through the production Bridge, as the enrollment route does. */
 public function actionEnroll() {
  $state=$this->state();
  foreach(['a','b'] as $name) {
   $credentials=Bridge::provision($state['humhub'][$name]['id'],hash('sha256','device-'.$name),'fixture-session-'.$name);
   if (($credentials['server']??'')!=='https://notifications.le-fil.net' || strlen($credentials['client_token']??'')<16) throw new \RuntimeException('Enrollment contract broken');
   $state['humhub'][$name]['token']=$credentials['client_token'];
  }
  $this->store($state);
  echo "HumHub members paired through the Le Fil bridge.\n";
 }
 private function gotify(string $token,string $path='/message?limit=100'):array {
  $curl=curl_init('http://127.0.0.1:18765'.$path);
  curl_setopt_array($curl,[CURLOPT_RETURNTRANSFER=>true,CURLOPT_HTTPHEADER=>['X-Gotify-Key: '.$token],CURLOPT_TIMEOUT=>5,CURLOPT_PROXY=>'']);
  $json=curl_exec($curl);$status=(int)curl_getinfo($curl,CURLINFO_RESPONSE_CODE);curl_close($curl);
  return [$status,$status===200?json_decode($json,true,64,JSON_THROW_ON_ERROR):null];
 }
 /** Proves the real HumHub chain: native Atrium notification and Messenger message reach only the recipient. */
 public function actionVerify() {
  $state=$this->state()['humhub'];
  [$status,$page]=$this->gotify($state['a']['token']);
  if ($status!==200) throw new \RuntimeException('Recipient history unreadable');
  $found=[];
  foreach($page['messages'] as $message) {
   $n=$message['extras']['lefil::notification']??[];
   if (str_contains(json_encode($message),'PRIVATE_BODY_SENTINEL')) throw new \RuntimeException('Private body leaked');
   $found[$n['source']??'']=$n;
  }
  if (!isset($found['atrium']) || !str_starts_with($found['atrium']['url'],'https://atrium.le-fil.net/index.php?r=notification%2Fentry&id=')) throw new \RuntimeException('Atrium notification missing: '.json_encode(array_keys($found)));
  if (!isset($found['talk']) || !preg_match('~^https://atrium\.le-fil\.net/index\.php\?r=mail%2Fmail%2Findex&id=[0-9]+$~D',$found['talk']['url'])) throw new \RuntimeException('Talk notification missing: '.json_encode(array_keys($found)));
  [$status,$other]=$this->gotify($state['b']['token']);
  if ($status!==200 || count($other['messages'])!==0) throw new \RuntimeException('Recipient privacy isolation failed');
  echo "Atrium and Talk delivered natively to the recipient only, without private text.\n";
 }
 /** Logging out of HumHub revokes the device token bound to that session. */
 public function actionLogout() {
  $state=$this->state()['humhub'];
  Bridge::logout($state['a']['id'],'fixture-session-a');
  [$status]=$this->gotify($state['a']['token']);
  if ($status!==401) throw new \RuntimeException('Logout did not revoke the device token (HTTP '.$status.')');
  [$status]=$this->gotify($state['b']['token']);
  if ($status!==200) throw new \RuntimeException('Logout of one member affected another');
  echo "Logout revoked only the member's own device.\n";
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
