
# CSL-Client (w-csl) Permission Handling with DBAPI Server

## Introduction
This documentation explains how permission handling works within the **CSL-Client (w-csl)**. It covers the integration between services (e.g., **discovery**, **autocrypt**) and the **dbapi server**. Each service defines a set of commands, and these commands are associated with certain privileges that are verified by the **dbapi server** when invoked via the HMI (Human Machine Interface).

---

## Structure Overview

### 1. **Commands in CSL-Client Services**
Each service within the CSL-Client (e.g., **discovery**, **autocrypt**) defines a set of commands. Each command is described by:
- **Endpoint Name**: The name of the command's API endpoint.
- **Method to be called**: The function or method responsible for executing the command.
- **JsonCmdHelp**: A JSON description of the command, including help text.
- **JsonCmdPrivilegeFamily**: The privilege family associated with the command to handle permission checking.

### 2. **Privilege Families in JsonCmdPrivilegeFamily Enum**
The **JsonCmdPrivilegeFamily** enum defines a set of privileges for managing different functionalities within the system. Each privilege family corresponds to a group of related commands. For example:
- **MANAGE_CERTIFICATE_AUTHORITY**: Privileges related to managing certificate authorities.

These privilege families are used to determine which permissions are needed to access certain commands.

---

## Command and Privilege Family Integration

### 3. **Defining Commands in Services**

In each service, commands are defined and associated with a privilege family. For example, in the **AutoCryptService**, two commands related to certificate authority management are defined as follows:

```
addCmd(AutoCryptEndpoints.GENERATE_ROOT_CA.cmd(),
        this::generateRootCA,
        AutoCryptEndpoints.GENERATE_ROOT_CA.help(),
        JsonCmdPrivilegeFamily.MANAGE_CERTIFICATE_AUTHORITY);

addCmd(AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA.cmd(),
        this::generateIntermediateCA,
        AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA.help(),
        JsonCmdPrivilegeFamily.MANAGE_CERTIFICATE_AUTHORITY);
```

Here, the commands `GENERATE_ROOT_CA` and `GENERATE_INTERMEDIATE_CA` are associated with the privilege family **MANAGE_CERTIFICATE_AUTHORITY**. This means that to execute these commands, the user or system must have the necessary permission tied to this privilege family.

### 4. **Permission Verification with DBAPI Server**

Once a command is associated with a **JsonCmdPrivilegeFamily**, it is sent to the **dbapi server** when invoked through the HMI. The **dbapi server** verifies whether the calling entity has the required permission for the specific privilege family.

For example:
- When the `GENERATE_ROOT_CA` or `GENERATE_INTERMEDIATE_CA` command is triggered, the dbapi server checks if the calling entity has **MANAGE_CERTIFICATE_AUTHORITY** permission.
- If the entity lacks the necessary privilege, the **dbapi server** denies access and prevents the operation from executing.

### 5. **How Permission Verification Works**

- When a command is executed via HMI, the **dbapi server** will receive the command and its associated **JsonCmdPrivilegeFamily**.
- The dbapi server will check if the requesting user has the appropriate privileges related to that family.
    - If the user has the required permission, the command is processed.
    - If not, the operation is blocked, and an error message is returned to the HMI.

---

## Example Breakdown

Consider the example in the **AutoCryptService** where two commands are added:

```
addCmd(AutoCryptEndpoints.GENERATE_ROOT_CA.cmd(),
        this::generateRootCA,
        AutoCryptEndpoints.GENERATE_ROOT_CA.help(),
        JsonCmdPrivilegeFamily.MANAGE_CERTIFICATE_AUTHORITY);

addCmd(AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA.cmd(),
        this::generateIntermediateCA,
        AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA.help(),
        JsonCmdPrivilegeFamily.MANAGE_CERTIFICATE_AUTHORITY);
```

- **Command**: `GENERATE_ROOT_CA` and `GENERATE_INTERMEDIATE_CA`
- **Method**: `this::generateRootCA` and `this::generateIntermediateCA`
- **Help**: `AutoCryptEndpoints.GENERATE_ROOT_CA.help()` and `AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA.help()`
- **Privilege Family**: `JsonCmdPrivilegeFamily.MANAGE_CERTIFICATE_AUTHORITY`

Once these commands are defined and linked to their privilege family, the **dbapi server** will verify permissions whenever they are invoked.

---

## Conclusion

The CSL-Client (w-csl) ensures that all commands are properly secured by associating them with relevant privilege families. This structure allows for robust permission handling when interacting with the **dbapi server**, ensuring that only authorized entities can access and execute sensitive operations.

---
