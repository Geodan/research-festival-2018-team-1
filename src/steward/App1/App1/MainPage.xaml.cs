using Plugin.Geolocator;
using Plugin.Permissions.Abstractions;
using System;
using System.Collections.Generic;
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
        private string broker = "iot.eclipse.org";
        private int publishInterval = 1;
        private IWifiIp wifiIp;
        private string deviceId;
        private int numberOfMesssages = 0;
        private bool haslocation = false;
        private bool continuePublishing = true;

        public MainPage()
        {
            InitializeComponent();

            var incidentTypes = new List<string>() { "Fire", "Bomb ", "Weather" };
            IncidentTypePicker.ItemsSource = incidentTypes;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();

            var hasPermission = await Utils.CheckPermissions(Permission.Location);

            Connectivity.ConnectivityChanged += Connectivity_ConnectivityChanged;
            wifiIp = DependencyService.Get<IWifiIp>();
            deviceId = wifiIp.GetDeviceId();

            Device.BeginInvokeOnMainThread(() =>
            {
                TxtDeviceId.Text = deviceId;
            });

            if (!hasPermission)
            {
                Application.Current?.MainPage?.DisplayAlert("Permissions", "please restart app", "Ok");
                return;
            }

            // start compass
            Compass.Start(SensorSpeed.Normal);
            Compass.ReadingChanged += Compass_ReadingChanged;

            TxtName.TextChanged += TxtName_TextChanged;
            name = TxtName.Text;

            TxtBroker.Text = broker;

            // start position 
            var locator = CrossGeolocator.Current;
            locator.DesiredAccuracy = 1;
            locator.PositionChanged += Locator_PositionChanged;
            await locator.StartListeningAsync(new TimeSpan(1), 1, true);

            if(Connectivity.NetworkAccess==NetworkAccess.Internet)
            {
                TxtHasInternetConnection.Text = "true";
                StartPublishing();
            }
            else
            {
                TxtHasInternetConnection.Text = "false";
            }

        }

        private void Connectivity_ConnectivityChanged(object sender, ConnectivityChangedEventArgs e)
        {
            if(e.NetworkAccess == NetworkAccess.Internet)
            {
                TxtHasInternetConnection.Text = "true";
                StartPublishing();
            }
            else
            {
                TxtHasInternetConnection.Text = "false";
                continuePublishing = false;
            }
        }

        private int GetIncidentTypeCode(string incidenttype)
        {
            switch (incidenttype)
            {
                case "Fire":
                    return 100;
                case "Bomb":
                    return 200;
                case "Weather":
                    return 300;
                default:return 0;
            }

        }


        public void StartPublishing()
        {
            continuePublishing = true;
            client = new MqttClient(broker);
            var clientId = Guid.NewGuid().ToString();
            client.Connect(clientId);

            Device.StartTimer(TimeSpan.FromSeconds(publishInterval), () =>
            {
                if (haslocation)
                {
                    var ip = wifiIp.GetWifiIp();

                    var dt = DateTime.UtcNow.ToString("o");
                    var message = $"{longitude},{latitude},{Math.Round(accuracy, 0)},{Math.Round(headingMagneticNorth, 0)},{ip},{name},{dt}";
                    client.Publish($"arena/{deviceId}", Encoding.Default.GetBytes(message));

                    numberOfMesssages++;
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        TxtIP.Text = ip;
                        TxtNumberOfMessages.Text = numberOfMesssages.ToString();
                    });
                }
                return continuePublishing; // True = Repeat again, False = Stop the timer
            });
        }

        private void Locator_PositionChanged(object sender, Plugin.Geolocator.Abstractions.PositionEventArgs e)
        {
            haslocation = true;
            accuracy = e.Position.Accuracy;
            longitude = e.Position.Longitude;
            latitude = e.Position.Latitude;

            Device.BeginInvokeOnMainThread(() =>
            {
                TxtLocation.Text = $"{Math.Round(longitude,4)}, {Math.Round(latitude,4)}, {Math.Round(accuracy,0)}";
            });
        }

        private void TxtName_TextChanged(object sender, TextChangedEventArgs e)
        {
            name = TxtName.Text;
        }

        private void Compass_ReadingChanged(object sender, CompassChangedEventArgs e)
        {
            var data = e.Reading;
            headingMagneticNorth = data.HeadingMagneticNorth;
            Device.BeginInvokeOnMainThread(() =>
            {
                TxtHeading.Text = Math.Round(headingMagneticNorth,0).ToString() + "°";
            });
        }

        void OnIncidentSendClicked(object sender, EventArgs args)
        {
            var dt = DateTime.UtcNow.ToString("o");
            var item = GetIncidentTypeCode((string)IncidentTypePicker.SelectedItem);
            var desc = IncidentTypeDescription.Text;
            if (desc == null)
            {
                desc = string.Empty;
            }

            var message = $"{deviceId},{name},{item},{desc},{longitude},{latitude},{dt}";
            client.Publish($"arena/incidents", Encoding.Default.GetBytes(message));

            DisplayAlert("Incident send", "Thanks, your Incident has been submitted.", "OK");
        }
    }
}
