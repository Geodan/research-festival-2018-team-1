using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class CamerControls : MonoBehaviour
{
    public float flySpeed = 1f;
    public float upDownSpeed = 1f;

    void Update()
    {
        CameraMovement();
    }

    void CameraMovement()
    {
        Vector3 moveDir = new Vector3(Input.GetAxis("Horizontal"), 0, Input.GetAxis("Vertical"));
        moveDir *= -flySpeed;

        if (Input.GetKey(KeyCode.Q))
        {
            moveDir.y += upDownSpeed;
        }
        else if(Input.GetKey(KeyCode.E))
        {
            moveDir.y -= upDownSpeed;
        }

        moveDir *= Time.deltaTime;

        transform.position += moveDir;
    }
}
