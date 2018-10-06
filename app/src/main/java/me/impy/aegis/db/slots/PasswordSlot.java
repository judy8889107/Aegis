package me.impy.aegis.db.slots;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.encoding.Hex;
import me.impy.aegis.encoding.HexException;

public class PasswordSlot extends RawSlot {
    private int _n;
    private int _r;
    private int _p;
    private byte[] _salt;

    public PasswordSlot() {
        super();
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject obj = super.toJson();
            obj.put("n", _n);
            obj.put("r", _r);
            obj.put("p", _p);
            obj.put("salt", Hex.encode(_salt));
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deserialize(JSONObject obj) throws SlotException {
        try {
            super.deserialize(obj);
            _n = obj.getInt("n");
            _r = obj.getInt("r");
            _p = obj.getInt("p");
            _salt = Hex.decode(obj.getString("salt"));
        } catch (JSONException | HexException e) {
            throw new SlotException(e);
        }
    }

    public SecretKey deriveKey(char[] password, byte[] salt, int n, int r, int p) {
        SecretKey key = CryptoUtils.deriveKey(password, salt, n, r, p);
        _n = n;
        _r = r;
        _p = p;
        _salt = salt;
        return key;
    }

    public SecretKey deriveKey(char[] password) {
        return CryptoUtils.deriveKey(password, _salt, _n, _r, _p);
    }

    @Override
    public byte getType() {
        return TYPE_DERIVED;
    }
}
