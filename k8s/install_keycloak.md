# Install Keycloak

```shell
kubectl apply -f ./keycloak/deployment.yaml
```

```shell
kubectl apply -f ./keycloak/ingress.yaml
```

___

# Configure Keycloak

Open in Browser:
[http://arch.homework/keycloak/](http://arch.homework/keycloak/)

Sign in user admin|admin
![keycloak-sign-in](./images/keycloak-sign-in.png)

Import Realm from file __[api-gateway-realm.json](./import/api-gateway-realm.json)__:
![keycloak-import-realm](./images/keycloak-import-realm.png)

Import Client from file __[api-gateway-client.json](./import/api-gateway-client.json)__:
![keycloak-import-client](./images/keycloak-import-client.png)

You don't need to fill or change any settings just click "Save":
![keycloak-import-client-select-file](./images/keycloak-import-client-2.png)

Imported realm __api-gateway__ has two preconfigured users: __jsnow|jsnow__ and __rstark|rstark__:
![keycloak-users](./images/keycloak_users.png)

You could register your own user go to this url:
[http://arch.homework/keycloak/realms/api-gateway/account](http://arch.homework/keycloak/realms/api-gateway/account)
and click __Register__ button:
![keycloak-user-registration](images/keycloak-user-registration.png)

You have to fill the registration form and click __register__:
![keycloak-registration-form.png](images/keycloak-registration-form.png)

Click __save__ button:
![keycloak-save-user](./images/keycloak-save-user.png)

Go to __[http://arch.homework/keycloak/admin/master/console/](http://arch.homework/keycloak/admin/master/console/)__ and
check that user successfully created:
![keycloak-user-registration-result](./images/keycloak-user-registration-result.png)

___

# Uninstall Keycloak

```shell
kubectl delete -f ./keycloak/deployment.yaml
```

```shell
kubectl delete -f ./keycloak/ingress.yaml
```
