## Install Postgresql

```shell
kubectl apply -f ./postgresql/
```

## Connect to Postgres

```shell
kubectl exec -it postgresql-5db9f48876-9v4n8 -- psql -h localhost -U test -p 5432 users
```

___

## Uninstall Postgresql

```shell
kubectl delete -f ./postgresql/
```
