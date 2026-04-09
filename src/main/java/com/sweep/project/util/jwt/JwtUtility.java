package com.sweep.project.util.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

import static com.sweep.project.util.jwt.TokenEnum.TOKEN_PREFIX;

@Component
public class JwtUtility {
    @Value("${jwt.secret-key}")
    private String signKey;

    @Value("${jwt.issuer}")
    private String issuer;

    private String makeToken(Long memberId, TokenEnum tokenCategory
            , Date date, Duration duration){
        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(date)
                .expiration(new Date(date.getTime()+duration.toMillis()))
                .claim("id",memberId)
                .claim("category",tokenCategory.getValue())
                .signWith(getSignKey())
                .compact();
    }
    public  String genRefreshToken(Long memberId){
        return makeToken(memberId, TokenEnum.REFRESH,new Date(),Duration.ofDays(30L));
    }
    public  String genAccessToken(Long memberId){
        return makeToken(memberId, TokenEnum.ACCESS,new Date(),Duration.ofDays(1L));
    }
    private SecretKey getSignKey(){
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(signKey));
    }


    private Claims getClaimsFromToken(String token){
        Jws<Claims> claims= Jwts.parser()
                .verifyWith(getSignKey()).build().parseSignedClaims(token);
        return claims.getPayload();
    }

    public Claims getClaims(String token){
        try{
            return getClaimsFromToken(token);
        }
        catch (ExpiredJwtException e){
            return e.getClaims();
        }
    }
    public boolean isExpire(String token){
        try {
            return Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token)
                    .getPayload().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
    public Boolean validToken(String token){
        try{
            Claims claims=getClaims(token);
            return true;
        }
        catch (ExpiredJwtException e) {
            return true;
        }
        catch (Exception e){
            return false;
        }
    }
    public String getTokenFromHeader(String authorizationHeader){
        if(authorizationHeader!=null&&authorizationHeader.startsWith(TOKEN_PREFIX.getValue())){
            return authorizationHeader.replace(TOKEN_PREFIX.getValue(),"");
        }
        return null;
    }
}
