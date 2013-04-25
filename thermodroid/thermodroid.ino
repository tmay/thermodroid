#include <i2cmaster.h>

/*
AndroidAccessory acc("DetroitLabs",
		     "ThermoDroid",
		     "Non contact 64 zone temp sensor",
		     "1.0",
		     "http://www.detroitlabs.com",
		     "0000000000000001");
*/
int tempPin = 0;

byte START_FLAG = 0x12;
byte END_FLAG = 0x13;
byte ESCAPE = 0x7D;
//arduino states
const byte READY = 0x01;
const byte IDLE = 0x02;
const byte RUN = 0x03;

//deafult state
byte state = READY;

/*
 * Attention! I commented out the alpha_ij array, so if you're going to compile the sketch you'll get for sure an error.
 * You should replace all 64 values with the alpha_ij calculated using the values stored in your MLX90620's EEPROM. 
 * I suggest you to make an EEPROM dump, print it on the Serial port and store it in a file. From there, with the help of a spreadsheet (Libreoffice, Google Docs, Excel...) calculate your own alpha_ij values. 
 * Please also pay attention to your emissivity value: since in my case it was equal to 1, to save SRAM i cut out that piece of calculation. You need to restore those lines if your emissivity value is not equal to 1. 
 */

int freq = 16;  //Set this value to your desired refresh frequency

int IRDATA[64];
byte CFG_LSB, CFG_MSB, PTAT_LSB, PTAT_MSB, CPIX_LSB, CPIX_MSB, PIX_LSB, PIX_MSB;
int PIX, v_th, CPIX;
float ta, to, emissivity, k_t1, k_t2;
float temperatures[64];
int count=0;
unsigned int PTAT;
int a_cp, b_cp, tgc, b_i_scale;

int a_ij[64];
int b_ij[64];
float alpha_ij[64] = {1.591E-8, 1.736E-8, 1.736E-8, 1.620E-8, 1.783E-8, 1.818E-8, 1.992E-8, 1.748E-8, 1.864E-8, 2.056E-8, 2.132E-8, 2.033E-8, 2.097E-8, 2.324E-8, 2.388E-8, 2.161E-8, 2.155E-8, 2.394E-8, 2.353E-8, 2.068E-8, 2.353E-8, 2.633E-8, 2.708E-8, 2.394E-8, 2.499E-8, 2.778E-8, 2.731E-8, 2.580E-8, 2.539E-8, 2.796E-8, 2.871E-8, 2.598E-8, 2.586E-8, 2.801E-8, 2.830E-8, 2.633E-8, 2.609E-8, 2.894E-8, 2.924E-8, 2.633E-8, 2.464E-8, 2.778E-8, 2.894E-8, 2.673E-8, 2.475E-8, 2.737E-8, 2.796E-8, 2.679E-8, 2.394E-8, 2.708E-8, 2.714E-8, 2.644E-8, 2.347E-8, 2.563E-8, 2.493E-8, 2.388E-8, 2.179E-8, 2.440E-8, 2.504E-8, 2.295E-8, 2.033E-8, 2.283E-8, 2.295E-8, 2.155E-8};  //<-- REPLACE THIS VALUES WITH YOUR OWN!
//float v_ir_off_comp[64];  //I'm going to merge v_ir_off_comp calculation into v_ir_tgc_comp equation. It's not required anywhere else, so I'll save 256 bytes of SRAM doing this.
float v_ir_tgc_comp[64];
//float v_ir_comp[64];		//removed to save SRAM, in my case v_ir_comp == v_ir_tgc_comp



void config_MLX90620_Hz(int Hz){
  byte Hz_LSB;
  switch(Hz){
    case 0:
      Hz_LSB = B00001111;
      break;
    case 1:
      Hz_LSB = B00001110;
      break;
    case 2:
      Hz_LSB = B00001101;
      break;
    case 4:
      Hz_LSB = B00001100;
      break;
    case 8:
      Hz_LSB = B00001011;
      break;
    case 16:
      Hz_LSB = B00001010;
      break;
    case 32:
      Hz_LSB = B00001001;
      break;
    default:
      Hz_LSB = B00001110;
  }
  i2c_start_wait(0xC0);
  i2c_write(0x03);    
  i2c_write((byte)Hz_LSB-0x55); 
  i2c_write(Hz_LSB);   
  i2c_write(0x1F);  
  i2c_write(0x74);  
  i2c_stop();
}

void read_EEPROM_MLX90620(){
  byte EEPROM_DATA[256];
  i2c_start_wait(0xA0);    
  i2c_write(0x00);
  i2c_rep_start(0xA1);
  for(int i=0;i<=255;i++){
    EEPROM_DATA[i] = i2c_readAck();
  }
  i2c_stop();
  varInitialization(EEPROM_DATA);
  write_trimming_value(EEPROM_DATA[247]);
}

void write_trimming_value(byte val){
  i2c_start_wait(0xC0);
  i2c_write(0x04); 
  i2c_write((byte)val-0xAA); 
  i2c_write(val);   
  i2c_write(0x56);  
  i2c_write(0x00);  
  i2c_stop();
}

void calculate_TA(){ 
  ta = (-k_t1 + sqrt(square(k_t1) - (4 * k_t2 * (v_th - (float)PTAT))))/(2*k_t2) + 25; 	//it's much more simple now, isn't it? :)
}

void calculate_TO(){
  float v_cp_off_comp = (float) CPIX - (a_cp + (b_cp/pow(2, b_i_scale)) * (ta - 25)); //this is needed only during the to calculation, so I declare it here.
  
  for (int i=0; i<64; i++){
    v_ir_tgc_comp[i] = IRDATA[i] - (a_ij[i] + (float)(b_ij[i]/pow(2, b_i_scale)) * (ta - 25)) - (((float)tgc/32)*v_cp_off_comp);
    //v_ir_comp[i]= v_ir_tgc_comp[i] / emissivity;									//removed to save SRAM, since emissivity in my case is equal to 1. 
    //temperatures[i] = sqrt(sqrt((v_ir_comp[i]/alpha_ij[i]) + pow((ta + 273.15),4))) - 273.15;
    temperatures[i] = sqrt(sqrt((v_ir_tgc_comp[i]/alpha_ij[i]) + pow((ta + 273.15),4))) - 273.15;	//edited to work with v_ir_tgc_comp instead of v_ir_comp
  }
}


void read_IR_ALL_MLX90620(){
  i2c_start_wait(0xC0);
  i2c_write(0x02);      
  i2c_write(0x00);     
  i2c_write(0x01);       
  i2c_write(0x40);       
  i2c_rep_start(0xC1);
  for(int i=0;i<=63;i++){
    PIX_LSB = i2c_readAck(); 
    PIX_MSB = i2c_readAck(); 
    IRDATA[i] = (PIX_MSB << 8) + PIX_LSB;
  }
  i2c_stop();
}

void read_PTAT_Reg_MLX90620(){
  i2c_start_wait(0xC0);
  i2c_write(0x02);
  i2c_write(0x90);
  i2c_write(0x00);
  i2c_write(0x01);
  i2c_rep_start(0xC1);
  PTAT_LSB = i2c_readAck();
  PTAT_MSB = i2c_readAck();
  i2c_stop();
  PTAT = ((unsigned int)PTAT_MSB << 8) + PTAT_LSB;
}

void read_CPIX_Reg_MLX90620(){
  i2c_start_wait(0xC0);
  i2c_write(0x02);
  i2c_write(0x91);
  i2c_write(0x00);
  i2c_write(0x01);
  i2c_rep_start(0xC1);
  CPIX_LSB = i2c_readAck();
  CPIX_MSB = i2c_readAck();
  i2c_stop();
  CPIX = (CPIX_MSB << 8) + CPIX_LSB;
}

void read_Config_Reg_MLX90620(){
  i2c_start_wait(0xC0);
  i2c_write(0x02);
  i2c_write(0x92);
  i2c_write(0x00);
  i2c_write(0x01);
  i2c_rep_start(0xC1);
  CFG_LSB = i2c_readAck();
  CFG_MSB = i2c_readAck();
  i2c_stop();
}

void check_Config_Reg_MLX90620(){
  read_Config_Reg_MLX90620();
  if ((!CFG_MSB & 0x04) == 0x04){
    config_MLX90620_Hz(freq);
  }
}

void varInitialization(byte EEPROM_DATA[]){
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


void setup(){
  pinMode(13, OUTPUT);
  Serial.begin(115200);
  Serial1.begin(115200);
  Serial.println("SETUP COMPLETE");
  i2c_init(); 
  //PORTC = (1 << PORTC4) | (1 << PORTC5);
  delay(5);
  read_EEPROM_MLX90620();
  config_MLX90620_Hz(freq);
  
}

void loop(){
  if (Serial1.peek() != -1) {  
    do {
      byte b = Serial1.read();
      state = b;
      Serial.println(b,HEX);
      //Serial.print((char) Serial.read());
    } while (Serial1.peek() != -1);
  }
  
  switch(state) {
    case READY:
      //Serial1.println("READY");
      break;
    case IDLE:
      Idle_Serial_Transmit();
      delay(1000);
      break;
    case RUN:
      if(count ==0){		//TA refresh is slower than the pixel readings, I'll read the values and computate them not every loop. 
         read_PTAT_Reg_MLX90620();
         calculate_TA();
         check_Config_Reg_MLX90620();
      }
      count++;
      if(count >=16){
        count = 0;
      }
      read_IR_ALL_MLX90620();
      read_CPIX_Reg_MLX90620();
      calculate_TO();
      Temperatures_Serial_Transmit();
      break;
  }
}

void Send_Escaped_Data(byte b) {
  if (b == START_FLAG  ||
      b == END_FLAG    ||
      b == ESCAPE) {
        Serial1.write(ESCAPE); 
  }
  Serial1.write(b);    
}

void Temperatures_Serial_Transmit(){
  Serial1.write(START_FLAG);
  for(int i=0;i<=63;i++){
    byte * b = (byte *) &temperatures[i];
    for (int j=0; j<4; j++) {
      Send_Escaped_Data(b[j]);
    }
  }
  Serial1.write(END_FLAG);
  //Serial.println("send");
}

void Idle_Serial_Transmit() {
  Serial1.write(START_FLAG);
  delay(10);
  Serial1.write(END_FLAG);
}

float getVoltage(int pin) {
 return (analogRead(tempPin) * .004882814); 
}
