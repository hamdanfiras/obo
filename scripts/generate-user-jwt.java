import com.example.obo.common.JwtUtils;
import com.nimbusds.jwt.SignedJWT;

public class GenerateUserJwt {
    public static void main(String[] args) throws Exception {
        String userId = args.length > 0 ? args[0] : "user-123";
        String email = args.length > 1 ? args[1] : "user@example.com";
        
        SignedJWT jwt = JwtUtils.createUserJwt(userId, email);
        String token = jwt.serialize();
        
        System.out.println("User JWT Token:");
        System.out.println(token);
        System.out.println();
        System.out.println("Use this token in the Authorization header:");
        System.out.println("Authorization: Bearer " + token);
    }
}

