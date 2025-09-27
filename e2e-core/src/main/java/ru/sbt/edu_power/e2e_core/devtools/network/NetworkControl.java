package ru.sbt.edu_power.e2e_core.devtools.network;

import com.github.kklisura.cdt.protocol.types.network.ConnectionType;
import ru.sbt.edu_power.e2e_core.devtools.DevTools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Утилитарный класс для работы с объектами Network, привязанными к конкретному потоку исполнения тестов
 */
public class NetworkControl {
    // Хранилище объектов NetworkActiveConnection привязанных к своему DevTools через его идентификатор
    private static final Map<Integer, NetworkActiveConnection> NETWORK_ACTIVE_CONNECTION_MAP = new ConcurrentHashMap<>();

    // Включение режима отслеживания ошибок с бэка
    public static void errorToastControlEnable() {
        getNetworkActiveConnectionInstance().enableErrorControl();
    }

    // Возвращает список неразобранных ошибок с бэка
    public static Map<String, String> getAppErrorLis() {
        return getNetworkActiveConnectionInstance()
                .getErrors()
                .stream()
                .collect(Collectors.toMap(id -> id, id -> getNetworkActiveConnectionInstance().getConnectionStat(id).getStackTrace()));
    }

    // Очищает список ошибок
    public static void clearAppErrorList() {
        getNetworkActiveConnectionInstance().clearErrors();
    }

    // Включает режим отслеживания активности коннектов к серверу
    public static void networkActiveConnectionEnable() {
        getNetworkActiveConnectionInstance().enable();
    }

    // Возвращает статус активности коннектов к серверу. true - активных коннектов нет
    public static boolean isConnectionNotActive() {
        return getNetworkActiveConnectionInstance().isConnectionNotActive();
    }

    // Включение предопределённых установок снижения скорости работы сетевого соединения
    public static void throttleNetwork() {
        DevTools.getNetwork().emulateNetworkConditions(false, 10D, 198000D, 98000D, ConnectionType.OTHER);
    }

    // Возвращает инстанс NetworkActiveConnection для текущего потока исполнения
    public static NetworkActiveConnection getNetworkActiveConnectionInstance() {
        if (!NETWORK_ACTIVE_CONNECTION_MAP.containsKey(DevTools.getId())) {
            NETWORK_ACTIVE_CONNECTION_MAP.put(DevTools.getId(), new NetworkActiveConnection(DevTools.getNetwork()));
        }
        return NETWORK_ACTIVE_CONNECTION_MAP.get(DevTools.getId());
    }
}
