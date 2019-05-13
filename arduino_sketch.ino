/* May 13, 2019, Barcelona
* sha256.h and sha256.cpp are taken from https://github.com/simonratner/Arduino-SHA-256
*/
#include "sha256.h"
#include<Servo.h>
//variables
Sha256 sha;
Servo servo;
char * strCode;//random char
String inString, inCmd, inCode, inHash, localHmacStr, strPepared;
int led = 13;
int servoPin = 8;
//timer
unsigned long VALIDE_TIME = 20000; // 20 sec
unsigned long delayStart = 0; // the time the delay started
bool delayRunning = false; // true if still waiting for delay to finish
//HMAC 256 characters
char myKey[] = "yyQNVTj-?V%McY$7Mx=UY_x2gMA=e^FRh7&^TFMHZ6P9kLdrDPLSZzV5eMf#J3-5U&FJ$gf*rkCJ8dTaFZr@!w9XcgNY2H?vjp^gxWuTWTg+!wcgJ63zVU$U+@My#+YbKL@jrRt528x+fCZ7EQ-P8EkAU&xTb3@y*XPpXE5u+#6g!Z&kzyQv!?r@fXTs3LMhSvx_VYkEm5DJzA5zTcH&Lytuxre_d7hPrZ9e6cfLT%6x?JZU2s&bBGbC2zv=ZU?w";
int keyLength = strlen(myKey);
void setup()
{
  Serial.begin(9600);
  uint8_t* hash;//for HMAC
  pinMode(led, OUTPUT);
  digitalWrite(led, LOW);
  servo.attach(servoPin); //servo motor
}
void loop()
{
  while (Serial.available())
  {
    //message received when paired
    inString = Serial.readString();
    if (inString == "x")
    {
      createRandomStr(10);//10 random numbers
      sendRanddomValues();//will also starts the timer
    }

    inCmd = inString.substring(0, 4); //Received command
    inHash = inString.substring(14);//Received HMAC SHA256 hash

    if (delayRunning && ((millis() - delayStart > VALIDE_TIME))) {
      strCode = 0;
      Serial.println("Time out!");
    }
    else if (delayRunning && ((millis() - delayStart <= VALIDE_TIME)))
    {
      //compute hash message
      strPepared = inCmd + String(strCode);
      sha.initHmac((uint8_t*)myKey, keyLength);
      sha.print(strPepared);
      localHmacStr = GetHash256(sha.resultHmac());
      localHmacStr.toLowerCase(); //to lower cases
      //valide hash
      if (localHmacStr == inHash)
      {
        //LED control command
        if (inCmd == "W5sb")
        {
          digitalWrite(led, HIGH);
          rotate_180();//rotate servo motor
        }
        if (inCmd == "xmG9")
        {
          //LED off
          digitalWrite(led, LOW);
          rotate_back();
        }
      } else {
        Serial.println("Token is invalid!");
      }
    }
  }
}

//send connect token/random codes over serial to BT module
void sendRanddomValues()
{
  Serial.print(strCode);
  Serial.print('~'); //used as an end of transmission character
  //start timer
  delayStart = millis();
  delayRunning = true;
}

//generate random string/char
void createRandomStr(int n)
{
  const int len = n;
  char song[] = {'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  const byte songLength = sizeof(song) / sizeof(song[0]);
  char notes[len + 1]; //allow an extra for NULL
  for (int n = 0; n < len ; n++)
  {
    notes[n] = song[random(0, songLength)];
    notes[n + 1] = '\0';
  }
  strCode = notes;
}

//HMAC function
String GetHash256(uint8_t* hash) {
  char tmp[16];
  String sHash256 = "";
  int i;
  for (i = 0; i < 32; i++) {
    sprintf(tmp, "%.2X", hash[i]);
    sHash256.concat(tmp);
  }
  return sHash256 ;
}
//format hash message
void printHash(uint8_t* hash) {
  int i;
  for (i = 0; i < 32; i++) {
    Serial.print("0123456789abcdef"[hash[i] >> 4]);
    Serial.print("0123456789abcdef"[hash[i] & 0xf]);
  }
  Serial.println();
}

//0 to 180 rotate
void rotate_180() {
  servo.write(180);
  delay(100);
}

void rotate_back() {
  servo.write(0);
  delay(100);
}