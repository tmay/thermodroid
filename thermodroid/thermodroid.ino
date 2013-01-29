#include <i2cmaster.h>
#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

AndroidAccessory acc("DetroitLabs",
		     "ThermoDroid",
		     "Non contact 64 zone temp sensor",
		     "1.0",
		     "http://www.detroitlabs.com",
		     "0000000000000001");

//consts
int I2C_DEVICE_ID  =  0xA0;

//mem
byte EEPROM_DATA[256];

//device vars
int PIX, CPIX;
float emissivity;

//VTH0 of absolute temperature sensor
//calculated from (Vth_H:0xDB(219) & Vth_L:0xDA(218))
int v_th;

//KT1 of absolute temperature sensor
//calculated from (Kt1_H:0xDD(221) & Kt1_L:0xDC(220))
int k_t1;

//KT2 of absolute temperature sensor
//calculated from (Kt2_H:0xDF(223) & Kt2_L:0xDE(222))
int k_t2;

//Ambient Temperature measured from the chip – (the package temperature)
float ta;

//Object Temperature, ‘seen’ from IR sensor
float to;

//(0xD4:212)Compensation pixel individual offset coefficients 
//2's complement
int a_cp;

//(0xD5:213)Individual Ta dependence (slope) of the compensation pixel offset
//2's complement
int b_cp;

//(0xD8:216)Thermal gradient coefficient
//2's complement
int tgc;

//(0xD9:217)Scaling coefficient for slope of IR pixels offset
//unsigned
int b_i_scale;

//(0x00...0x3F:0...63)IR pixel individual offset coefficient
int a_ij[64];

//(0x40...0x7F:64...127)Individual Ta dependence (slope) of IR pixels offset
int b_ij[64];

const byte DUMP_EEPROM = 100;

void setup() {
  Serial.begin(115200);
  i2c_init();
  acc.powerOn();
  Serial.println("Power ON");
}

void loop() {
  
  byte msg[3];
  int value = 10;
  if (acc.isConnected()) {
    boolean hasMessage = acc.read(msg, sizeof(msg), 1) > 0;
    if (hasMessage) {
      switch(msg[0]) {
        case DUMP_EEPROM:
          Serial.println("READING EEPROM DATA");
          readEEPROM();
          Serial.println("SENDING EEPROM DATA");
          acc.write(EEPROM_DATA, sizeof(EEPROM_DATA)-1);
          break;
      }  
    }
  } else {
    //no connection, wat do?
  }
}

void readEEPROM() {
  i2c_start_wait(I2C_DEVICE_ID);
  //Dump command 
  
  i2c_write(0x00);
  i2c_rep_start(0xA1);
  Serial.println("Start EEPROM Read");
  for(int i=0; i<256; i++) {
    EEPROM_DATA[i] = i2c_readAck();
  }
  i2c_stop();  
}

void initDeviceVars(byte EEPROM_DATA[]){
  v_th = (EEPROM_DATA[219] <<8) + EEPROM_DATA[218];
  k_t1 = ((EEPROM_DATA[221] <<8) + EEPROM_DATA[220])/1024.0;
  k_t2 =((EEPROM_DATA[223] <<8) + EEPROM_DATA[222])/1048576.0;
  
  a_cp = EEPROM_DATA[212];
  if(a_cp > 127){
    a_cp = a_cp - 256;
  }
  b_cp = EEPROM_DATA[213];
  if(b_cp > 127){
    b_cp = b_cp - 256;
  }
  tgc = EEPROM_DATA[216];
  if(tgc > 127){
    tgc = tgc - 256;
  }

  b_i_scale = EEPROM_DATA[217];

  emissivity = (((unsigned int)EEPROM_DATA[229] << 8) + EEPROM_DATA[228])/32768.0;

  for(int i=0;i<=63;i++){
    a_ij[i] = EEPROM_DATA[i];
    if(a_ij[i] > 127){
      a_ij[i] = a_ij[i] - 256;
    }
    b_ij[i] = EEPROM_DATA[64+i];
    if(b_ij[i] > 127){
      b_ij[i] = b_ij[i] - 256;
    }
  }
}
