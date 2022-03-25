package com.beemdevelopment.aegis.otp;

import android.net.Uri;

import com.beemdevelopment.aegis.GoogleAuthProtos;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GoogleAuthInfo implements Serializable {
    public static final String SCHEME = "otpauth";
    public static final String SCHEME_EXPORT = "otpauth-migration";

    private OtpInfo _info;
    private String _accountName;
    private String _issuer;

    public GoogleAuthInfo(OtpInfo info, String accountName, String issuer) {
        _info = info;
        _accountName = accountName;
        _issuer = issuer;
    }

    public static GoogleAuthInfo parseUri(String s) throws GoogleAuthInfoException {
        Uri uri = Uri.parse(s);
        if (uri == null) {
            throw new GoogleAuthInfoException(uri, String.format("Bad URI format: %s", s));
        }
        return GoogleAuthInfo.parseUri(uri);
    }

    /* 檢查輸入進去的 uri */
    public static GoogleAuthInfo parseUri(Uri uri) throws GoogleAuthInfoException {
        String scheme = uri.getScheme();
        /* SCHEME = "otpauth"，2FA提供的 QRcode掃出來前面都是這個開頭
        * otpauth://totp/ID:100001534662130?secret=PVPVDQLV6RM3EQLLAV6NRROOB5F6N2US&digits=6&issuer=Facebook */
        if (scheme == null || !scheme.equals(SCHEME)) {
            throw new GoogleAuthInfoException(uri, String.format("Unsupported protocol: %s", scheme));
        }

        // 'secret' is a required parameter
        /* 將 String型別的URL轉變為URI
        * 利用URI的getQueryParameter方法獲取引數
        * 這裡是密鑰號碼 */
        String encodedSecret = uri.getQueryParameter("secret");
        if (encodedSecret == null) {
            throw new GoogleAuthInfoException(uri, "Parameter 'secret' is not present");
        }

        byte[] secret;
        try {
            secret = parseSecret(encodedSecret);
        } catch (EncodingException e) {
            throw new GoogleAuthInfoException(uri, "Bad secret", e);
        }
        /* type parameter */
        OtpInfo info;
        String issuer = "";
        try {
            /* uri.getHost()本來是返回主機名，但是 2FA QRcode掃描出的內容長這樣
            * otpauth://totp/ID:100001534662130?secret=PVPVDQLV6RM3EQLLAV6NRROOB5F6N2US&digits=6&issuer=Facebook
            * 所以主機明會自動變成中間 totp 那段 */
            String type = uri.getHost();
            if (type == null) {
                throw new GoogleAuthInfoException(uri, String.format("Host not present in URI: %s", uri.toString()));
            }

            switch (type) {
                case "totp":
                    TotpInfo totpInfo = new TotpInfo(secret);
                    String period = uri.getQueryParameter("period");
                    if (period != null) {
                        totpInfo.setPeriod(Integer.parseInt(period));
                    }
                    info = totpInfo;
                    break;
                case "steam":
                    SteamInfo steamInfo = new SteamInfo(secret);
                    period = uri.getQueryParameter("period");
                    if (period != null) {
                        steamInfo.setPeriod(Integer.parseInt(period));
                    }
                    info = steamInfo;
                    break;
                case "hotp":
                    HotpInfo hotpInfo = new HotpInfo(secret);
                    String counter = uri.getQueryParameter("counter");
                    if (counter == null) {
                        throw new GoogleAuthInfoException(uri, "Parameter 'counter' is not present");
                    }
                    hotpInfo.setCounter(Long.parseLong(counter));
                    info = hotpInfo;
                    break;
                case YandexInfo.HOST_ID:
                    String pin = uri.getQueryParameter("pin");
                    if (pin != null) {
                        pin = new String(parseSecret(pin), StandardCharsets.UTF_8);
                    }

                    info = new YandexInfo(secret, pin);
                    issuer = info.getType();
                    break;
                default:
                    throw new GoogleAuthInfoException(uri, String.format("Unsupported OTP type: %s", type));
            }
        } catch (OtpInfoException | NumberFormatException | EncodingException e) {
            throw new GoogleAuthInfoException(uri, e);
        }

        // provider info used to disambiguate accounts
        /* 這裡getPath會得到
        * otpauth://totp/ID:100001534662130?secret=PVPVDQLV6RM3EQLLAV6NRROOB5F6N2US&digits=6&issuer=Facebook
        * /ID:100001534662130 這段 */
        String path = uri.getPath();


        String label = path != null && path.length() > 0 ? path.substring(1) : "";


        String accountName = "";
        String issuerParam = uri.getQueryParameter("issuer");

        if (label.contains(":")) {
            // a label can only contain one colon
            // it's ok to fail if that's not the case
            /* Judy修復 Facebook issuer Bug問題 */
            /* 出錯的地方在這裡，若有 :則直接以:前面的值為issuer
            * otpauth://totp/ID:100001534662130?secret=PVPVDQLV6RM3EQLLAV6NRROOB5F6N2US&digits=6&issuer=Facebook */
            String[] strings = label.split(":");
            if (strings.length == 2) {
                issuer = strings[0];
                accountName = strings[1];
                /* issuerParam和issuer都不為空，去比較兩個字串的值，並以issuerParam為主 */
                if(issuerParam!=null && issuer!=null && !issuerParam.equals(issuer)){
                    issuer = issuerParam;
                }
            } else {
                // at this point, just dump the whole thing into the accountName
                accountName = label;
            }
        } else {
            // label only contains the account name
            // grab the issuer's info from the 'issuer' parameter if it's present
            /* 這裡則是針對在path得到的資料，若沒有issuer(沒有 :)才去填入issuer
            * otpauth://totp/ID:100001534662130?secret=PVPVDQLV6RM3EQLLAV6NRROOB5F6N2US&digits=6&issuer=Facebook */
//            String issuerParam = uri.getQueryParameter("issuer");

            if (issuer.isEmpty()) {
                issuer = issuerParam != null ? issuerParam : "";
            }

            accountName = label;
        }

        // just use the defaults if these parameters aren't set
        try {
            String algorithm = uri.getQueryParameter("algorithm");
            if (algorithm != null) {
                info.setAlgorithm(algorithm);
            }
            String digits = uri.getQueryParameter("digits");
            if (digits != null) {
                info.setDigits(Integer.parseInt(digits));
            }
        } catch (OtpInfoException | NumberFormatException e) {
            throw new GoogleAuthInfoException(uri, e);
        }

        return new GoogleAuthInfo(info, accountName, issuer);
    }

    /**
     * Decodes the given base 32 secret, while being tolerant of whitespace and dashes.
     */
    public static byte[] parseSecret(String s) throws EncodingException {
        s = s.trim().replace("-", "").replace(" ", "");
        return Base32.decode(s);
    }

    public static Export parseExportUri(String s) throws GoogleAuthInfoException {
        Uri uri = Uri.parse(s);
        if (uri == null) {
            throw new GoogleAuthInfoException(uri, "Bad URI format");
        }
        return GoogleAuthInfo.parseExportUri(uri);
    }

    public static Export parseExportUri(Uri uri) throws GoogleAuthInfoException {
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(SCHEME_EXPORT)) {
            throw new GoogleAuthInfoException(uri, "Unsupported protocol");
        }

        String host = uri.getHost();
        if (host == null || !host.equals("offline")) {
            throw new GoogleAuthInfoException(uri, "Unsupported host");
        }

        String data = uri.getQueryParameter("data");
        if (data == null) {
            throw new GoogleAuthInfoException(uri, "Parameter 'data' is not set");
        }

        GoogleAuthProtos.MigrationPayload payload;
        try {
            byte[] bytes = Base64.decode(data);
            payload = GoogleAuthProtos.MigrationPayload.parseFrom(bytes);
        } catch (EncodingException | InvalidProtocolBufferException e) {
            throw new GoogleAuthInfoException(uri, e);
        }

        List<GoogleAuthInfo> infos = new ArrayList<>();
        for (GoogleAuthProtos.MigrationPayload.OtpParameters params : payload.getOtpParametersList()) {
            OtpInfo otp;
            try {
                int digits;
                switch (params.getDigits()) {
                    case DIGIT_COUNT_UNSPECIFIED:
                        // intentional fallthrough
                    case DIGIT_COUNT_SIX:
                        digits = TotpInfo.DEFAULT_DIGITS;
                        break;
                    case DIGIT_COUNT_EIGHT:
                        digits = 8;
                        break;
                    default:
                        throw new GoogleAuthInfoException(uri, String.format("Unsupported digits: %d", params.getDigits().ordinal()));
                }

                String algo;
                switch (params.getAlgorithm()) {
                    case ALGORITHM_UNSPECIFIED:
                        // intentional fallthrough
                    case ALGORITHM_SHA1:
                        algo = "SHA1";
                        break;
                    case ALGORITHM_SHA256:
                        algo = "SHA256";
                        break;
                    case ALGORITHM_SHA512:
                        algo = "SHA512";
                        break;
                    default:
                        throw new GoogleAuthInfoException(uri, String.format("Unsupported hash algorithm: %d", params.getAlgorithm().ordinal()));
                }

                byte[] secret = params.getSecret().toByteArray();
                switch (params.getType()) {
                    case OTP_TYPE_UNSPECIFIED:
                        // intentional fallthrough
                    case OTP_TYPE_TOTP:
                        otp = new TotpInfo(secret, algo, digits, TotpInfo.DEFAULT_PERIOD);
                        break;
                    case OTP_TYPE_HOTP:
                        otp = new HotpInfo(secret, algo, digits, params.getCounter());
                        break;
                    default:
                        throw new GoogleAuthInfoException(uri, String.format("Unsupported algorithm: %d", params.getType().ordinal()));
                }
            } catch (OtpInfoException e) {
                throw new GoogleAuthInfoException(uri, e);
            }

            String name = params.getName();
            String issuer = params.getIssuer();
            int colonI = name.indexOf(':');
            if (issuer.isEmpty() && colonI != -1) {
                issuer = name.substring(0, colonI);
                name = name.substring(colonI + 1);
            }

            GoogleAuthInfo info = new GoogleAuthInfo(otp, name, issuer);
            infos.add(info);
        }

        return new Export(infos, payload.getBatchId(), payload.getBatchIndex(), payload.getBatchSize());
    }

    public OtpInfo getOtpInfo() {
        return _info;
    }

    public Uri getUri() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(SCHEME);

        if (_info instanceof TotpInfo) {
            if (_info instanceof SteamInfo) {
                builder.authority("steam");
            } else if (_info instanceof YandexInfo) {
                builder.authority(YandexInfo.HOST_ID);
            } else {
                builder.authority("totp");
            }
            builder.appendQueryParameter("period", Integer.toString(((TotpInfo) _info).getPeriod()));
        } else if (_info instanceof HotpInfo) {
            builder.authority("hotp");
            builder.appendQueryParameter("counter", Long.toString(((HotpInfo) _info).getCounter()));
        } else {
            throw new RuntimeException(String.format("Unsupported OtpInfo type: %s", _info.getClass()));
        }

        builder.appendQueryParameter("digits", Integer.toString(_info.getDigits()));
        builder.appendQueryParameter("algorithm", _info.getAlgorithm(false));
        builder.appendQueryParameter("secret", Base32.encode(_info.getSecret()));

        if (_info instanceof YandexInfo) {
            builder.appendQueryParameter("pin", Base32.encode(((YandexInfo) _info).getPin()));
        }

        if (_issuer != null && !_issuer.equals("")) {
            builder.path(String.format("%s:%s", _issuer, _accountName));
            builder.appendQueryParameter("issuer", _issuer);
        } else {
            builder.path(_accountName);
        }

        return builder.build();
    }

    public String getIssuer() {
        return _issuer;
    }

    public String getAccountName() {
        return _accountName;
    }

    public static class Export {
        private int _batchId;
        private int _batchIndex;
        private int _batchSize;
        private List<GoogleAuthInfo> _entries;

        public Export(List<GoogleAuthInfo> entries, int batchId, int batchIndex, int batchSize) {
            _batchId = batchId;
            _batchIndex = batchIndex;
            _batchSize = batchSize;
            _entries = entries;
        }

        public List<GoogleAuthInfo> getEntries() {
            return _entries;
        }

        public int getBatchSize() {
            return _batchSize;
        }

        public int getBatchIndex() {
            return _batchIndex;
        }

        public int getBatchId() {
            return _batchId;
        }
    }
}
