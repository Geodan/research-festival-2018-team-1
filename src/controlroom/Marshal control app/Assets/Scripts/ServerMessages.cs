using System;
using System.Collections.Concurrent; 
using System.Text;
using UnityEngine;
using uPLibrary.Networking.M2Mqtt;
using uPLibrary.Networking.M2Mqtt.Messages;

public class ServerMessages : MonoBehaviour
{
    private string broker = "broker.hivemq.com";
    //private int port = 1883;
    private string topic = "arena/steward1";
    private string message = "this is a test. dont panic!";

    MqttClient client;

    ConcurrentQueue<float> queue = new ConcurrentQueue<float>();

    public GameObject agentObject;

    void Start()
    {
        client = new MqttClient(broker);
        client.MqttMsgPublishReceived += client_MqttMsgPublishReceived;
        var clientId = Guid.NewGuid().ToString();
        client.Connect(clientId);
        client.Subscribe(new[] { topic }, new[] { MqttMsgBase.QOS_LEVEL_EXACTLY_ONCE });

    }

    // Use this for initialization
    void Update()
    {
        if (Input.GetButtonDown("Fire1"))
        {
            SendServerMessage("arena/test", "This is a test. Don't panic!");
        }

        if (queue.Count > 0)
        {
            float angle;
            queue.TryDequeue(out angle);
            AgentRotation(angle);
        }
    }

    public void SendServerMessage(string topicName, string serverMessage)
    {
        client.Publish(topic, Encoding.Default.GetBytes(message));
    }

    private void client_MqttMsgPublishReceived(object sender, MqttMsgPublishEventArgs e)
    {
        var s = Encoding.Default.GetString(e.Message);

        var sSplit = s.Split(',');

        if(sSplit[0] == "head")
        {
            s = sSplit[1];

            var f = float.Parse(s);

            queue.Enqueue(f);

            Debug.Log(f);
        }
    }

    public  void AgentRotation(float angle) //Rotate Agent
    {
        agentObject.transform.eulerAngles = new Vector3(transform.eulerAngles.x, angle, transform.eulerAngles.z);
    }
}

