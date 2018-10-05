using UnityEngine;

public class MouseInteraction : MonoBehaviour
{
    private Camera cam;

    public string agentString = "Agent";
    public string baseGroundString = "BaseGround";
    public string targetPosString = "TargetPos";

    void Start()
    {
        InitialReferences();
    }

    void InitialReferences()
    {
        cam = Camera.main;
    }

    void Update()
    {
        ClickInteract();
    }

    void ClickInteract()
    {
        if (Input.GetButtonDown("Fire1"))
        {
            Ray ray = cam.ScreenPointToRay(Input.mousePosition);
            RaycastHit rayHit;

            if (Physics.Raycast(ray, out rayHit, 1000))
            {
                if (rayHit.transform.CompareTag(agentString))
                {
                    //Interact with agent
                }
                else if(rayHit.transform.CompareTag(baseGroundString))
                {
                    GetComponent<TargetLocation>().SetNewTargetLoc(rayHit.point);
                }
                else if (rayHit.transform.CompareTag(targetPosString))
                {
                    GetComponent<TargetLocation>().DestroyTargetObject(rayHit.transform.gameObject);
                }
            }
        }
    }
}
