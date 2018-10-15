package de.openfiresource.openpager.models;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.openfiresource.openpager.models.api.Device;
import de.openfiresource.openpager.models.api.OpenPagerService;
import de.openfiresource.openpager.models.api.TokenInterceptor;
import de.openfiresource.openpager.models.api.UserKey;
import de.openfiresource.openpager.models.api.UserLogin;
import de.openfiresource.openpager.utils.Constants;
import de.openfiresource.openpager.utils.Preferences;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

@Singleton
public class Rest {

    private final OpenPagerService service;

    @Inject
    Rest(Preferences preferences) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Timber.tag("OkHttp").v(message));
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new TokenInterceptor(preferences))
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BACKEND_URL_API)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        service = retrofit.create(OpenPagerService.class);
    }

    Single<UserKey> login(UserLogin userLogin) {
        return service.login(userLogin).subscribeOn(Schedulers.io());
    }

    Completable logout() {
        return service.logout().subscribeOn(Schedulers.io());
    }

    Completable putDeviceInfo(Device device) {
        return service.putDeviceInfo(device.getFcmToken(), device).subscribeOn(Schedulers.io());
    }
}