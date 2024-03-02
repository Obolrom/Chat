Instructions for successful building of project

`To setup https connection`
1) Start a cluster - https://github.com/Obolrom/WishesSpringBootApp (docker-compose.yaml)
2) Copy generated ./ssl/certificate.pem to android res->raw->certificate
3) Get ip address of cluster machine. E.g in linux `hostname -I`)
4) Change ip address of a cluster in NetworkModule.CLUSTER_IP_ADDRESS property.
