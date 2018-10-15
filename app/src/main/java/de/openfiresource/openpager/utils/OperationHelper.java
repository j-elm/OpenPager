package de.openfiresource.openpager.utils;

import android.text.TextUtils;

import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.openfiresource.openpager.models.AppDatabase;
import de.openfiresource.openpager.models.database.OperationMessage;
import de.openfiresource.openpager.models.database.OperationRule;
import io.reactivex.Single;
import timber.log.Timber;

import static de.openfiresource.openpager.utils.EncryptionUtils.decrypt;

public class OperationHelper {

    public static Single<OperationMessage> createOperationFromFCM(Preferences preferences, AppDatabase database, Map<String, String> extras) {
        return Single.create(emitter -> {
            OperationMessage incoming = new OperationMessage();
            Set<String> keys = extras.keySet();
            boolean encryption = preferences.isSyncEncryptionEnabled();
            String encryptionKey = preferences.getSyncEncryptionPassword();

            try {
                for (String string : keys) {
                    String value = URLDecoder.decode(extras.get(string), EncryptionUtils.CHARACTER_ENCODING);

                    if (TextUtils.isEmpty(value)) {
                        continue;
                    }

                    switch (string) {
                        case "key":
                            if (encryption) {
                                value = decrypt(value, encryptionKey);
                            }
                            incoming.setKey(value);
                            break;
                        case "title":
                            if (encryption) {
                                value = decrypt(value, encryptionKey);
                            }
                            incoming.setTitle(value);
                            break;
                        case "message":
                            if (encryption) {
                                value = decrypt(value, encryptionKey);
                            }
                            incoming.setMessage(value);
                            break;
                        case "destination":
                            if (encryption) {
                                value = decrypt(value, encryptionKey);
                            }
                            incoming.setLatlng(value);
                            break;
                        case "time":
                            if (encryption) {
                                value = decrypt(value, encryptionKey);
                            }
                            try {
                                long timestamp = Long.parseLong(value);
                                incoming.setTimestamp(new Date(timestamp * 1000));
                            } catch (NumberFormatException e) {
                                Timber.e(e, "Error parsing incoming date");
                            }
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception exception) {
                Timber.e(exception, "Error parsing incoming Operation");
                emitter.onError(exception);
                return;
            }

            //We can't do anything without message or title
            if (incoming.getMessage() == null || incoming.getTitle() == null) {
                emitter.onError(new IllegalArgumentException("No Title or Message provided"));
                return;
            }

            //Get the rule
            OperationRule bestRule = null;
            SimpleDateFormat dateFormatTime = new SimpleDateFormat("HH:mm", Locale.GERMAN);
            Calendar now = Calendar.getInstance();

            for (OperationRule rule : database.operationRuleDao().getAll()) {
                Calendar startValue = GregorianCalendar.getInstance();
                Calendar stopValue = GregorianCalendar.getInstance();
                try {
                    Date start = dateFormatTime.parse(rule.getStartTime());
                    Date stop = dateFormatTime.parse(rule.getStopTime());

                    startValue.setTime(start);
                    stopValue.setTime(stop);

                    //No (Milli)Seconds -> 00:00 to 23:59
                    startValue.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                    startValue.set(Calendar.SECOND, 0);
                    startValue.set(Calendar.MILLISECOND, 0);
                    stopValue.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                    stopValue.set(Calendar.MILLISECOND, 0);
                    stopValue.set(Calendar.SECOND, 0);

                    //22:00 to 06:00 as example. 06:00 is next night
                    if (start.after(stop) && start.after(now.getTime())) {
                        startValue.add(Calendar.DATE, -1);
                    } else if (start.after(stop) && start.before(now.getTime())) {
                        stopValue.add(Calendar.DATE, +1);
                    }
                } catch (ParseException e) {
                    Timber.e(e, "Error parsing start/stop time");
                    e.printStackTrace();
                }

                // is rule in date?
                if (now.compareTo(startValue) >= 0 && now.compareTo(stopValue) <= 0) {
                    // is SearchText in message?
                    if (TextUtils.isEmpty(rule.getSearchText())
                            || incoming.getTitle().matches(rule.getSearchText())
                            || incoming.getMessage().matches(rule.getSearchText())) {

                        // Higher priority
                        if (bestRule == null || bestRule.getPriority() < rule.getPriority()) {
                            bestRule = rule;
                        }
                    }
                }
            }

            Timber.d("Incoming operation: %s\n%s", incoming.getTitle(), incoming.getMessage());

            incoming.setAlarm(true);
            incoming.setSeen(false);
            incoming.setTimestampIncoming(now.getTime());

            if (bestRule != null) {
                incoming.setOperationRuleId(bestRule.getId());
            }

            if (incoming.getTimestamp() == null) {
                incoming.setTimestamp(now.getTime());
            }

            emitter.onSuccess(incoming);
        });
    }
}