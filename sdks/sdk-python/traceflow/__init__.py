import os
from .tracer import TracerClient

class Tracer:
    instance = None

    @classmethod
    def init(cls, service_name=None, api_key=None, collector_url=None):
        if cls.instance:
            return cls.instance

        service_name = service_name or os.environ.get("TRACER_SERVICE_NAME")
        api_key = api_key or os.environ.get("TRACER_API_KEY")
        collector_url = collector_url or os.environ.get("TRACER_COLLECTOR_URL", "http://localhost:4318")

        if not service_name:
            raise ValueError("Tracer requires a service_name")
        if not api_key:
            raise ValueError("Tracer requires an api_key")

        cls.instance = TracerClient(service_name, api_key, collector_url)
        return cls.instance
