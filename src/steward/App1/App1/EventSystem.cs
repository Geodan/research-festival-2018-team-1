using System;
using System.Diagnostics;
using System.Text;
using uPLibrary.Networking.M2Mqtt;

namespace App1
{
    public class EventSystem
    {
        private double headingNorth;
        private MqttClient client;
        private string topic = "arena";
        public string Name;

        public void Start()
        {
            var broker = "broker.hivemq.com";
            var port = 1883;

            client = new MqttClient(broker);
            var clientId = Guid.NewGuid().ToString();
            client.Connect(clientId);
        }

        private string GetTopic()
        {
            return $"{topic}/{Name}";
        }

        public void AccelerometerEvent(double x, double y, double z)
        {
            if (client == null) return;
            //Debug.WriteLine($"acc,{x},{y},{z}");
            client.Publish(GetTopic(), Encoding.Default.GetBytes($"acc,{Math.Round(x, 3)},{Math.Round(y, 3)},{Math.Round(z, 3)}"));
        }

        public void CompassEvent(double heading)
        {
            if (client == null) return;
            //Debug.WriteLine("Headingnorth:" + heading);
            client.Publish(GetTopic(), Encoding.Default.GetBytes($"head,{Math.Round(heading, 0)}"));
        }

        public void StepEvent(float steps)
        {
            if (client == null) return;
            //Debug.WriteLine("step");
            client.Publish(GetTopic(), Encoding.Default.GetBytes($"step,{steps}"));
        }
    }
}
