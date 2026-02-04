# Infrastructure Command Center Overhaul

## Engineering Goals
- [ ] Implement non-blocking "Instant-UI" with Shimmer/Placeholder nodes.
- [ ] Overhaul `InfrastructureScreenModel` to use parallel coroutine probing (Semaphore-capped).
- [ ] Fix probe accuracy by injecting standard Anikku headers and using `HttpSource` interceptors.
- [ ] Create a `SourceHealthCache` to sync status between Statistics and the Browse section.
- [ ] Add "Deep Diagnostics" log for failed probes (showing HTTP Error codes).

## Technical Requirements
- Use `Injekt` to get `NetworkHelper`.
- Use `Flow` to stream individual node updates to the UI as they finish.
- Ensure the "Green Dot" in Browse pulls from the `SourceHealthCache`.
