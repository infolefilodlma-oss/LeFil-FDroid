<?php
declare(strict_types=1);
require_once __DIR__ . '/../server/shared/Broker.php';

use LeFil\Notifications\Broker;

$state = [];
$load = static fn(int $uid): ?string => $state[$uid] ?? null;
$save = static function(int $uid, string $ciphertext) use (&$state): void { $state[$uid] = $ciphertext; };
$broker = new Broker('humhub', $load, $save);
$deviceA = hash('sha256', 'isolated-device-A');
$deviceB = hash('sha256', 'isolated-device-B');
$a = $broker->provision(2001, $deviceA, 'fixture-session-A');
$b = $broker->provision(2002, $deviceB, 'fixture-session-B');
if ($a['subject'] === $b['subject'] || $a['client_token'] === $b['client_token']) {
    throw new RuntimeException('Cross-recipient token collision');
}
if ($broker->provision(2001, $deviceA, 'fixture-session-A')['client_token'] !== $a['client_token']) {
    throw new RuntimeException('Idempotent enrollment broken');
}
$payload = [
    'title'=>'Le Fil',
    'message'=>'Nouvelle notification Atrium',
    'priority'=>5,
    'extras'=>['lefil::notification'=>[
        'source'=>'atrium','event_id'=>'humhub:fixture-101',
        'url'=>'https://atrium.le-fil.net/index.php?r=notification%2Fentry&id=101'
    ]]
];
if (!$broker->deliver(2001,$payload)) throw new RuntimeException('Delivery failed');
$api = static function(string $token): array {
    $curl = curl_init('http://127.0.0.1:18765/message?limit=100');
    curl_setopt_array($curl, [
        CURLOPT_RETURNTRANSFER=>true,CURLOPT_HTTPHEADER=>['X-Gotify-Key: '.$token],
        CURLOPT_TIMEOUT=>5,CURLOPT_PROXY=>''
    ]);
    $json=curl_exec($curl);$status=curl_getinfo($curl,CURLINFO_RESPONSE_CODE);curl_close($curl);
    if ($json === false || $status !== 200) throw new RuntimeException('Gotify client history not readable');
    return json_decode($json,true,512,JSON_THROW_ON_ERROR)['messages'] ?? [];
};
$messagesA=$api($a['client_token']);
$messagesB=$api($b['client_token']);
if (count($messagesA)!==1 || count($messagesB)!==0) throw new RuntimeException('Recipient privacy isolation failed');
$actual=$messagesA[0]['extras']['lefil::notification']??[];
if (($actual['event_id']??'')!=='humhub:fixture-101' || ($actual['source']??'')!=='atrium') {
    throw new RuntimeException('Android contract mismatch');
}
if (str_contains(json_encode($messagesA), 'PRIVATE_BODY_SENTINEL')) throw new RuntimeException('Private body leaked');
$broker->revoke(2001,$deviceA);
try {
    $broker->deliver(2001,$payload);
    throw new RuntimeException('Revocation not enforced');
} catch (RuntimeException $e) {
    if ($e->getMessage()==='Revocation not enforced') throw $e;
}
echo "Broker enrollment, Gotify delivery, payload contract, recipient isolation and revocation verified.\n";
