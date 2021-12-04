//LED燈亮5秒為口罩、溫度都通過
//蜂鳴器響5秒表示口罩沒戴好
//蜂鳴器以0.5秒響3聲表示溫度過高
//蜂鳴器響2組"一長一短"表示溫度過低或未檢測到溫度

#include <SPI.h>
#include <MFRC522.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>

constexpr uint8_t RST_PIN = D3;     // 腳位設定
constexpr uint8_t SS_PIN = D4;      // 腳位設定

const char* ssid = "yang0001";              //Wifi設定
const char* password = "yangyang0821";      //Wifi設定
const char* mqtt_server = "140.131.152.17"; //Server設定
const char* clientID = "yangyang";          //MQTT用ID
const int   mqtt_port = 1883;               //MQTT專用port
int messagecont = 0;                        //訊息計數

WiFiClient espClient;
PubSubClient client(espClient);
MFRC522 rfid(SS_PIN, RST_PIN); // Instance of the class
MFRC522::MIFARE_Key key;//create a MIFARE_Key struct named 'key', which will hold the card information

void callback(char* topic, byte *payload, unsigned int length) {    //設定callback，刷卡時啟動判斷

  String message;
  String message2;

   if (strcmp(topic,"Mask")==0){                //topic為"Mask"時啟動
    Serial.print("Mask Message:");
    for (int i = 0; i < length; i++) {
        message = message + (char) payload[i];  // convert *byte to string
    }
    Serial.print(message);                      //顯示收到之口罩辨識訊息
    if(message == "ON"){messagecont++;}         //收到訊息為"ON"時計數器+1
    if(messagecont>1){                          
      Serial.print("Door Open");                //計數器為2時顯示"Doorr Open"
     digitalWrite(D2, HIGH);                    //亮5秒表示溫度口罩都通過
     delay(5000);
     digitalWrite(D2, LOW);
     messagecont = 0;                           //計數器歸零，以方便下個人刷卡測量
     }
     }
 
  if (strcmp(topic,"Temp")==0) {                //topic為"Temp"時啟動
    Serial.print("Temp Message:");
    for (int j = 0; j < length; j++) {
        message2 = message2 + (char) payload[j];// convert *byte to string
    }
    Serial.print(message2);                     //顯示收到之溫度感測訊息
    if(message2 == "ON"){messagecont++;}        //收到訊息為"ON"時計數器+1
    if(messagecont>1){
      Serial.print("Door Open");                //計數器為2時顯示"Doorr Open"
     digitalWrite(D2, HIGH);                    //亮5秒表示溫度口罩都通過
     delay(5000);
     digitalWrite(D2, LOW);
     messagecont = 0;                           //計數器歸零，以方便下個人刷卡測量
     }
  }

     if(message == "OFF"){                      //收到口罩辨識為"OFF"時
     Serial.print("\t\t\t您的口罩沒戴好!");
     digitalWrite(D8, HIGH);                     //響5秒表示口罩沒戴好
     delay(5000);
     digitalWrite(D8, LOW);
     messagecont = 0;                           //計數器歸零
      }
     if(message2 == "OFF"){                     //收到溫度辨識為"OFF"時
     Serial.print("\t\t\t您的溫度過高!");
     digitalWrite(D8, HIGH);                    //蜂鳴器以每0.5秒響3聲表示溫度過高
     delay(1000);
     digitalWrite(D8, LOW);
     delay(500);
     digitalWrite(D8, HIGH);
     delay(1000);
     digitalWrite(D8, LOW);
     delay(500);
     digitalWrite(D8, HIGH);
     delay(1000);
     digitalWrite(D8, LOW);
     messagecont = 0;                           //計數器歸零
      }
     if(message2 == "off"){                     //收到溫度辨識為"off"時
     Serial.print("\t\t\t您的溫度過低，或者尚未檢測到溫度");
     digitalWrite(D8, HIGH);                    //蜂鳴器響兩組1短響1長響表示溫度過低或未檢測到溫度
     delay(300);
     digitalWrite(D8, LOW);
     delay(300);
     digitalWrite(D8, HIGH);
     delay(2000);
     digitalWrite(D8, LOW);
     delay(1000);
     digitalWrite(D8, HIGH);
     delay(300);
     digitalWrite(D8, LOW);
     delay(300);
     digitalWrite(D8, HIGH);
     delay(2000);
     digitalWrite(D8, LOW);
     delay(1000);
     messagecont = 0;                            //計數器歸零
      }

    Serial.println();
    Serial.println("-----------------------");

}

void setup() {
    pinMode(D8,OUTPUT);                         //D8為輸出腳位
    pinMode(D2,OUTPUT);
    Serial.begin(115200);                        //監控視窗用115200頻率

    WiFi.begin(ssid, password);                  //核對Wifi名稱&密碼
    while (WiFi.status() != WL_CONNECTED) {      //連接Wifi期間以0.5秒頻率顯示連接訊息
        delay(500);
        Serial.println("Connecting to WiFi..");
    }
    Serial.println("Connected to the WiFi network");
     Serial.println(WiFi.localIP());

    client.setServer(mqtt_server, mqtt_port);    //核對MQTT server、port
    client.setCallback(callback);                //callback函式
    while (!client.connected()) {
        Serial.println("Connecting to MQTT server .....");
       if (client.connect(clientID)) {           //使用ID連接MQTT server
         Serial.println("MQTTt server connected");
     }else {
            Serial.print("failed with state \n"); //失敗時顯示連線失敗
            Serial.print(client.state());
            delay(2000);
        }
    }
  while (!Serial);                                // Do nothing if no serial port is opened (added for Arduinos based on ATMEGA32U4)
  SPI.begin();                                    // Init SPI bus
  rfid.PCD_Init();                                // Init MFRC522
  rfid.PCD_DumpVersionToSerial();                 // Show details of PCD - MFRC522 Card Reader details
  Serial.println(F("Scan PICC to see UID, type, and data blocks..."));


    client.publish("Mask", "Start");              //向"Mask"的topic傳送訊息已表示裝置已啟動
    client.subscribe("Mask");                     //訂閱"Mask"的topic
    client.publish("Temp", "Start");              //向"Temp"的topic傳送訊息已表示裝置已啟動
    client.subscribe("Temp");                     //訂閱"Temp"的topic
}
  
void loop() {

client.loop();

  if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {   //感測到新卡片時
  Serial.print(F("Card UID:"));
      dump_byte_array(rfid.uid.uidByte, rfid.uid.size);               // 顯示卡片的UID
      Serial.println();
      Serial.print(F("PICC type: "));
      MFRC522::PICC_Type piccType = rfid.PICC_GetType(rfid.uid.sak);
      Serial.println(rfid.PICC_GetTypeName(piccType));                 //顯示卡片的類型

      rfid.PICC_HaltA();                                               // 卡片進入停止模式

      client.subscribe("Mask");
      client.publish("Mask", "ON OR OFF");                            //向"Mask"傳遞訊息詢問ON或OFF
      Serial.print("\nAsk arrived in topic: Mask\n");
      client.subscribe("Temp");
      client.publish("Temp", "ON or OFF?");                            //向"Temp"傳遞訊息詢問ON或OFF
      Serial.print("\nAsk arrived in topic: Temp\n");
      delay(4000);                                                     //詢問時間間隔為3秒，避免卡片連續被感測

}
}

// 這個副程式把讀取到的UID，用16進位顯示出來
void dump_byte_array(byte *buffer, byte bufferSize) {
  for (byte i = 0; i < bufferSize; i++) {
    Serial.print(buffer[i] < 0x10 ? " 0" : " ");
    Serial.print(buffer[i], HEX);
  }
}