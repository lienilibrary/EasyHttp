package com.lieni.library.easyhttp.cookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CookieJarHelper implements CookieJar {
    private SetCookieCache cache;
    private SharedPrefsCookiePersist persist;

    public CookieJarHelper(Context context, boolean load) {
        this.cache = new SetCookieCache();
        this.persist = new SharedPrefsCookiePersist(context);
        if(load) this.cache.addAll(persist.loadAll());
    }

    public CookieJarHelper(Context context){
        this(context,true);
    }

    @Override
    synchronized public void saveFromResponse(@NotNull HttpUrl url, @NotNull List<Cookie> cookies) {
        cache.addAll(cookies);
        persist.saveAll(filterPersistentCookies(cookies));
    }

    private static List<Cookie> filterPersistentCookies(List<Cookie> cookies) {
        List<Cookie> persistentCookies = new ArrayList<>();

        for (Cookie cookie : cookies) {
            if (cookie.persistent()) {
                persistentCookies.add(cookie);
            }
        }
        return persistentCookies;
    }

    @NotNull
    @Override
    synchronized public List<Cookie> loadForRequest(@NotNull HttpUrl url) {
        List<Cookie> cookiesToRemove = new ArrayList<>();
        List<Cookie> validCookies = new ArrayList<>();

        for (Iterator<Cookie> it = cache.iterator(); it.hasNext(); ) {
            Cookie currentCookie = it.next();

            if (isCookieExpired(currentCookie)) {
                cookiesToRemove.add(currentCookie);
                it.remove();

            } else if (currentCookie.matches(url)) {
                validCookies.add(currentCookie);
            }
        }

        persist.removeAll(cookiesToRemove);
        return validCookies;
    }

    private static boolean isCookieExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }


    synchronized public void clearSession() {
        cache.clear();
        cache.addAll(persist.loadAll());
    }

    private static class SetCookieCache implements Iterable<Cookie> {

        private Set<IdentifiableCookie> cookies;

        SetCookieCache() {
            cookies = new HashSet<>();
        }


        void addAll(Collection<Cookie> newCookies) {
            List<IdentifiableCookie> identifiableCookies = new ArrayList<>(cookies.size());
            for (Cookie cookie : newCookies) {
                identifiableCookies.add(new IdentifiableCookie(cookie));
            }

            for (IdentifiableCookie cookie : identifiableCookies) {
                this.cookies.remove(cookie);
                this.cookies.add(cookie);
            }
        }


        void clear() {
            cookies.clear();
        }

        @NotNull
        @Override
        public Iterator<Cookie> iterator() {
            return new SetCookieCacheIterator();
        }

        private class SetCookieCacheIterator implements Iterator<Cookie> {

            private Iterator<IdentifiableCookie> iterator;

            SetCookieCacheIterator() {
                iterator = cookies.iterator();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Cookie next() {
                return iterator.next().getCookie();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        }
    }

    private static class IdentifiableCookie {
        private Cookie cookie;

        IdentifiableCookie(Cookie cookie) {
            this.cookie = cookie;
        }

        Cookie getCookie() {
            return cookie;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof IdentifiableCookie)) return false;
            IdentifiableCookie that = (IdentifiableCookie) other;
            return that.cookie.name().equals(this.cookie.name())
                    && that.cookie.domain().equals(this.cookie.domain())
                    && that.cookie.path().equals(this.cookie.path())
                    && that.cookie.secure() == this.cookie.secure()
                    && that.cookie.hostOnly() == this.cookie.hostOnly();
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = 31 * hash + cookie.name().hashCode();
            hash = 31 * hash + cookie.domain().hashCode();
            hash = 31 * hash + cookie.path().hashCode();
            hash = 31 * hash + (cookie.secure() ? 0 : 1);
            hash = 31 * hash + (cookie.hostOnly() ? 0 : 1);
            return hash;
        }
    }

    private static class SharedPrefsCookiePersist {

        private final SharedPreferences sharedPreferences;

        SharedPrefsCookiePersist(Context context) {
            this(context.getSharedPreferences("CookiePersistence", Context.MODE_PRIVATE));
        }

        SharedPrefsCookiePersist(SharedPreferences sharedPreferences) {
            this.sharedPreferences = sharedPreferences;
        }


        List<Cookie> loadAll() {
            List<Cookie> cookies = new ArrayList<>(sharedPreferences.getAll().size());

            for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
                String serializedCookie = (String) entry.getValue();
                Cookie cookie = new SerializableCookie().decode(serializedCookie);
                if (cookie != null) {
                    cookies.add(cookie);
                }
            }
            return cookies;
        }


        void saveAll(Collection<Cookie> cookies) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (Cookie cookie : cookies) {
                editor.putString(createCookieKey(cookie), new SerializableCookie().encode(cookie));
            }
            editor.apply();
        }


        void removeAll(Collection<Cookie> cookies) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (Cookie cookie : cookies) {
                editor.remove(createCookieKey(cookie));
            }
            editor.apply();
        }

        private static String createCookieKey(Cookie cookie) {
            return (cookie.secure() ? "https" : "http") + "://" + cookie.domain() + cookie.path() + "|" + cookie.name();
        }
    }

    private static class SerializableCookie implements Serializable {
        private static final String TAG = SerializableCookie.class.getSimpleName();

        private static final long serialVersionUID = -8594045714036645534L;

        private transient Cookie cookie;

        String encode(Cookie cookie) {
            this.cookie = cookie;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = null;

            try {
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(this);
            } catch (IOException e) {
                Log.d(TAG, "IOException in encodeCookie", e);
                return null;
            } finally {
                if (objectOutputStream != null) {
                    try {
                        // Closing a ByteArrayOutputStream has no effect, it can be used later (and is used in the return statement)
                        objectOutputStream.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Stream not closed in encodeCookie", e);
                    }
                }
            }

            return byteArrayToHexString(byteArrayOutputStream.toByteArray());
        }

        private static String byteArrayToHexString(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte element : bytes) {
                int v = element & 0xff;
                if (v < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        }

        Cookie decode(String encodedCookie) {

            byte[] bytes = hexStringToByteArray(encodedCookie);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                    bytes);

            Cookie cookie = null;
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new ObjectInputStream(byteArrayInputStream);
                cookie = ((SerializableCookie) objectInputStream.readObject()).cookie;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return cookie;
        }

        private static byte[] hexStringToByteArray(String hexString) {
            int len = hexString.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character
                        .digit(hexString.charAt(i + 1), 16));
            }
            return data;
        }

        private static long NON_VALID_EXPIRES_AT = -1L;

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(cookie.name());
            out.writeObject(cookie.value());
            out.writeLong(cookie.persistent() ? cookie.expiresAt() : NON_VALID_EXPIRES_AT);
            out.writeObject(cookie.domain());
            out.writeObject(cookie.path());
            out.writeBoolean(cookie.secure());
            out.writeBoolean(cookie.httpOnly());
            out.writeBoolean(cookie.hostOnly());
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            Cookie.Builder builder = new Cookie.Builder();

            builder.name((String) in.readObject());

            builder.value((String) in.readObject());

            long expiresAt = in.readLong();
            if (expiresAt != NON_VALID_EXPIRES_AT) {
                builder.expiresAt(expiresAt);
            }

            final String domain = (String) in.readObject();
            builder.domain(domain);

            builder.path((String) in.readObject());

            if (in.readBoolean())
                builder.secure();

            if (in.readBoolean())
                builder.httpOnly();

            if (in.readBoolean())
                builder.hostOnlyDomain(domain);

            cookie = builder.build();
        }
    }
}
