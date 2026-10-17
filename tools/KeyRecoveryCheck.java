import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
public class KeyRecoveryCheck {
 public static void main(String[] args) throws Exception {
  char[] password=Files.readString(Path.of(args[1])).strip().toCharArray();
  try {
   KeyStore store=KeyStore.getInstance("PKCS12");
   try(var input=Files.newInputStream(Path.of(args[0]))){store.load(input,password);}
   if(!store.isKeyEntry("lefil-release"))throw new Exception("Signing alias unavailable");
   PrivateKey key=(PrivateKey)store.getKey("lefil-release",password);
   X509Certificate certificate=(X509Certificate)store.getCertificate("lefil-release");
   String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
   if(!fingerprint.equals("d0755310dbd84319bbd0cb658475168cb9234cd1b60f9893a092afa19c31b902"))throw new Exception("Unexpected signing identity");
   byte[] challenge=new byte[64];new SecureRandom().nextBytes(challenge);
   Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(key);signer.update(challenge);byte[] signature=signer.sign();
   Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(certificate);verifier.update(challenge);
   if(!verifier.verify(signature))throw new Exception("Recovered private key cannot sign correctly");
   System.out.println("{\"restored_pkcs12_opens\":true,\"private_key_sign_and_verify\":true,\"certificate_sha256\":\""+fingerprint+"\",\"new_key_generated\":false}");
  } finally {Arrays.fill(password,'\0');}
 }
}
