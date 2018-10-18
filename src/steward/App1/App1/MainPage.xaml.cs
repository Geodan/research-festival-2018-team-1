using Plugin.Geolocator;
using Plugin.Permissions.Abstractions;
using System;
using System.Text;
using uPLibrary.Networking.M2Mqtt;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace App1
{
    public partial class MainPage : ContentPage
    {
        private string name;
        private double headingMagneticNorth;
        private double longitude;
        private double latitude;
        private double accuracy;

        private MqttClient client;
        private string broker = "broker.hivemq.com";
        private int publishInterval = 1;

        public MainPage()
        {
            InitializeComponent();
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();

            var hasPermission = await Utils.CheckPermissions(Permission.Location);

            if (!hasPermission)
                return;

            // start compass
            Compass.Start(SensorSpeed.Normal);
            Compass.ReadingChanged += Compass_ReadingChanged;

            TxtName.TextChanged += TxtName_TextChanged;
            name = TxtName.Text;

            // start position 
            var locator = CrossGeolocator.Current;
            locator.DesiredAccuracy = 1;
            locator.PositionChanged += Locator_PositionChanged;
            await locator.StartListeningAsync(new TimeSpan(1), 1, true);


            StartPublishing();
        }

        public void StartPublishing()
        {
            client = new MqttClient(broker);
            var clientId = Guid.NewGuid().ToString();
            client.Connect(clientId);

            Device.StartTimer(TimeSpan.FromSeconds(publishInterval), () =>
            {
                var message = $"{longitude},{latitude},{accuracy},{headingMagneticNorth}";
                client.Publish($"arena/{name}", Encoding.Default.GetBytes(message));
                return true; // True = Repeat again, False = Stop the timer
            });
        }

        private void Locator_PositionChanged(object sender, Plugin.Geolocator.Abstractions.PositionEventArgs e)
        {
            accuracy = e.Position.Accuracy;
            longitude = e.Position.Longitude;
            latitude = e.Position.Latitude;
        }

        private void TxtName_TextChanged(object sender, TextChangedEventArgs e)
        {
            name = TxtName.Text;
        }

        private void Compass_ReadingChanged(object sender, CompassChangedEventArgs e)
        {
            var data = e.Reading;
            headingMagneticNorth = data.HeadingMagneticNorth;
        }
    }
}
