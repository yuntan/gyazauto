package xyz.untan.gyazauto;

import com.os.operando.garum.annotations.Pref;
import com.os.operando.garum.annotations.PrefKey;
import com.os.operando.garum.models.PrefModel;


@Pref(name = "app_status")
class AppStatus extends PrefModel{
    @PrefKey
    String accessToken;
}
