# Kubernetes manifests (practical starter)

Local development remains Docker Compose. These manifests are for staging/prod-shaped deploys.

## Layout

```text
infra/k8s/
  namespace.yaml
  configmap-common.yaml
  secrets.example.yaml
  services/          # one Deployment+Service per Spring app
  ingress.yaml
```

## Rules

- Never commit real secrets — copy `secrets.example.yaml` → sealed secret / external secret store.
- Each service has readiness `/actuator/health` and liveness probes.
- API traffic enters via Ingress → `api-gateway:8080`.
- Databases/Kafka/Keycloak are external (managed or Compose/Helm charts) — not embedded in app pods.

## Apply (example)

```bash
kubectl apply -f infra/k8s/namespace.yaml
kubectl apply -f infra/k8s/configmap-common.yaml
kubectl apply -f infra/k8s/secrets.example.yaml   # replace values first
kubectl apply -f infra/k8s/services/
kubectl apply -f infra/k8s/ingress.yaml
```

Validated structure only unless a local cluster is available.
