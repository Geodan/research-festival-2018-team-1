using UnityEngine;

public class AgentInteraction : MonoBehaviour
{
    public GameObject agentObject;

    public void AgentRotation(string angleString)
    {
        float rotAngle = float.Parse(angleString);
        agentObject.transform.eulerAngles = new Vector3(transform.eulerAngles.x, rotAngle, transform.eulerAngles.z);
    }
}
