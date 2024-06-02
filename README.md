# role-rest-service
This role service will save user roles for a organization.

## Run locally using profile
Use the following to run local profile which will pick up properties defined in the `application-local.yml` :

```
./gradlew bootRun --args="--spring.profiles.active=local"
```

For intellij set `spring.profiles.active=local` in environment variables in Run.

## Build Docker image
Build docker image using included Dockerfile.
`docker build -t imageregistry/role-rest-service:1.0 .` 

## Push Docker image to repository
`docker push imageregistry/role-rest-service:1.0`

## Deploy Docker image locally
`docker run -e POSTGRES_USERNAME=dummy \
 -e POSTGRES_PASSWORD=dummy -e POSTGRES_DBNAME=account \
  -e POSTGRES_SERVICE=localhost:5432 \
 --publish 8080:8080 imageregistry/role-rest-service:1.0`


## Installation on Kubernetes
Use my Helm chart here @ [sonam-helm-chart](https://github.com/sonamsamdupkhangsar/sonam-helm-chart):

```
helm install project-api sonam/mychart -f values.yaml --version 0.1.21 --namespace=yournamespace
```

##Instruction for port-forwarding database pod
```
export PGMASTER=$(kubectl get pods -o jsonpath={.items..metadata.name} -l application=spilo,cluster-name=role-minimal-cluster,spilo-role=master -n backend); 
echo $PGMASTER;
kubectl port-forward $PGMASTER 6432:5432 -n backend;
```

###Login to database instruction
```
export SECRET_FILE=sonam.role-minimal-cluster.credentials.postgresql.acid.zalan.do
export PGPASSWORD=$(kubectl get secret $SECRET_FILE -o 'jsonpath={.data.password}' -n backend | base64 -d);
echo $PGPASSWORD;
export PGSSLMODE=require;
psql -U <user> -d role -h localhost -p 6432

```