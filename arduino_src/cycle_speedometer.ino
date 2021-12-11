#define RX 10         //This will be used as software serial Receive Pin
#define TX 11         //This will be used as software serial Transmit Pin
#define ARR_LEN 10    //Defines How many samples will be used to compute RPM
#define REED_PIN 2    //Pin that gives updates on the Reed Switch

#include <SoftwareSerial.h> 
SoftwareSerial MyBlue(RX, TX); // RX | TX 

const byte interruptPin = REED_PIN; //Defining the Interrupt pin 

float radius_tyre=0.4; // in meters
float avg_time=0; //Computes the average time between 2 samples 
int loop_count=0; //To count number of loops
int bluetooth_delay_factor=200;   // This helps us in slowing down the rate at which we send the bluetooth signal
int index=0;      //Indicates at what index will current sample be stored in the output

int start_flag=0; // Flag to handle initial speed issue
unsigned long  old_time=0;  // sampled time in previous cycle
unsigned long  new_time=0;  // sampled time in current cycle
unsigned long  temp_time=0;  // Dummy varaible used to remove some debouncing problems (Manually Configured)

float time_interval[ARR_LEN];
float avg_rpm=0;
int avg_speed=0;
int m=1;    // Represents the number of samples per revolution

void setup() 
{   
  //Serial.begin(9600);   //Uncomment if need any debugging
  MyBlue.begin(9600);   // Starting the Bluetooth connection
  pinMode(REED_PIN,INPUT);
  //Serial.println("Ready to connect\nDefualt password is 1234 or 000");  // To connect to HC05
  attachInterrupt(digitalPinToInterrupt(interruptPin), interval_capture, RISING);  // Configuring to use only rising edge as trigger to interrupt
} 


void loop() 
{ 
   //Serial.println(digitalRead(REED_PIN));
  if(millis()-temp_time>4500) // Handling the idle case. If exceeded this amount of time then speed will be zeroed
  {
    for(int i=0;i<ARR_LEN;i++)
        time_interval[i]=100; // Hundered here represents a large number
  }
  float sum=0;
  if (start_flag==0){avg_time=average(time_interval,index);}
  else
  {avg_time=average(time_interval,ARR_LEN);}

  if (loop_count%bluetooth_delay_factor==0 and MyBlue) {  // Regime to send data through bluetooth
    avg_rpm= m*60/avg_time;   // RPM is estimated using Average time
    avg_speed= avg_rpm*(2*3.14159/60)*radius_tyre * (18/5);  // v=r*w (in kmph)
    MyBlue.println(avg_speed);  // Sending speed in KMPH
  }
  loop_count++;
  delay(1);
} 

float average(float arr[],int n){   //Function to compute average. Made sure that interrupts sample time is much longer than the runtime of this module
  float sum=0;
  for(int i=0;i<n;i++)
    sum+=arr[i];
  return sum*1.0/n;
}

void interval_capture(){    // Interrupt Service Routine
  old_time=new_time;
  temp_time=millis();
  if (temp_time-old_time < 30) { return; }  // sampling time more than 30ms - to eliminate false positives (Reducing Debouncing) - an upper limit of 60kmph
  new_time=temp_time;
  if(index>=ARR_LEN)
  {
    start_flag=1;
    index=0;
  }
  time_interval[index%ARR_LEN]=(new_time-old_time)/1000.0;  // updating the array (in seconds)
  index+=1;
  
}
