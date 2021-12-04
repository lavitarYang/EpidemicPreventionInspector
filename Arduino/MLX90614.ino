#include <Adafruit_MLX90614.h>
#include <Wire.h>

#include <ESP8266WiFi.h>
#include <PubSubClient.h>

Adafruit_MLX90614 mlx = Adafruit_MLX90614(); // 宣告MLX90614為mlx

// WiFi
const char *ssid = "Jack77531";              // 輸入WiFi名稱
const char *password = "jack77531";          // 輸入WiFi密碼

// MQTT Broker
const char *mqtt_broker = "140.131.152.17";  // 伺服器位址
const char *topic = "Temp";                  // Topic
const char *client_id = "Temp";              // Client名稱
const int mqtt_port = 1883;                  // Port

String message;                              // 訊息

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
    Serial.begin(115200);                    // 設定頻率為115200
    WiFi.begin(ssid, password);              // 連線到WiFi
    while (WiFi.status() != WL_CONNECTED) {  // WiFi連線中
        delay(1000);
        Serial.println("Connecting to WiFi...");
    }
    client.setServer(mqtt_broker, mqtt_port);     // 連線到MQTT Broker端
    client.setCallback(callback);                 // 在Client端使用callback函式
    while (!client.connected()) {                       // 持續迴圈直到連上
        if (client.connect(client_id)) {
            Serial.println("MQTT broker connected\n");  // 連線成功輸出"MQTT broker connected"
        }
        else {
            Serial.print("Failed with state");          // 連線失敗輸出"Failed with state"
            Serial.print(client.state());
            delay(2000);
        }
    }
    
    // subscribe topic
    client.subscribe(topic);
}

void callback(char *topic, byte *payload, unsigned int length) {    // 接收Topic發布訊息
    Serial.print("Message arrived in topic: ");
    Serial.println(topic);
    Serial.print("Message:");
    for (int i = 0; i < length; i++) {
        message = message + (char) payload[i];
    }
    Serial.println(message);    
    Serial.println("-----------------------");
}

void loop() {
   client.loop();                   // 持續使用Client端
   if(message=="ON or OFF?"){
     mlx.begin();
     Serial.print("Ambient = ");
     Serial.print(mlx.readAmbientTempC());   // 輸出偵測出的周圍溫度
     Serial.println("*C");
     Serial.print("Object = ");
     Serial.print(mlx.readObjectTempC());    // 輸出偵測出的待測物溫度
     Serial.println("*C");
     if(mlx.readObjectTempC() < 28){         // 判斷是否低於28度，如果低於則輸出"off"
       client.publish(topic, "off");
     }
     else if(mlx.readObjectTempC() < 37.5){  // 判斷是否高於28度且小於37.5度，如果是則輸出"ON"
       client.publish(topic, "ON");
     }
     else{                                   // 判斷是否高於37.5度，如果高於則輸出"OFF"
       client.publish(topic, "OFF");
     }
   }
}
