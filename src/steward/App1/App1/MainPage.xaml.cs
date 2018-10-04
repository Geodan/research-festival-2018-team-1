using System;
using System.Diagnostics;
using System.Text;
using uPLibrary.Networking.M2Mqtt;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace App1
{
    public partial class MainPage : ContentPage
    {
        private double headingNorth;
        private MqttClient client;
        string topic = "arena/test";

        public MainPage()
        {
            InitializeComponent();
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();

            // start compass
            Compass.Start(SensorSpeed.UI);
            Compass.ReadingChanged += Compass_ReadingChanged;

            var broker = "broker.hivemq.com";
            var port = 1883;

            client = new MqttClient(broker);
            var clientId = Guid.NewGuid().ToString();
            client.Connect(clientId);
        }


        void Compass_ReadingChanged(object sender, CompassChangedEventArgs e)
        {
            var data = e.Reading;
            headingNorth = data.HeadingMagneticNorth;
            Debug.WriteLine("Headingnorth:" + headingNorth);
            client.Publish(topic, Encoding.Default.GetBytes(headingNorth.ToString()));
        }
    }
}
