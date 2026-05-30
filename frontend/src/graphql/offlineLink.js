import { ApolloLink, Observable } from '@apollo/client';
import offlineService from '../services/offlineService';

/**
 * Custom Apollo Link that handles offline operations
 * 
 * This link intercepts GraphQL operations and:
 * 1. Lets queries pass through (they'll fail if offline)
 * 2. Queues mutations when offline
 * 3. Prevents redundant queueing by checking context
 */
export const offlineLink = new ApolloLink((operation, forward) => {
  // Skip if this operation is already being retried from the queue
  if (operation.getContext().skipQueue) {
    return forward(operation);
  }

  // If we're online, let the operation go through normally
  if (offlineService.getNetworkStatus().isOnline) {
    return forward(operation);
  }

  // We're offline - handle based on operation type
  const operationType = operation.query.definitions[0]?.operation;
  
  // For mutations, queue them for later execution
  if (operationType === 'mutation') {
    console.log('Detected offline mutation, queueing for later', operation.operationName);
    
    // Queue the mutation for later
    return new Observable(observer => {
      // Store the operation in the offline queue
      offlineService.queueOperation({
        type: 'MUTATION',
        payload: {
          client: operation.getContext().client,
          mutation: operation.query,
          variables: operation.variables
        },
        context: operation.getContext(),
        operationName: operation.operationName,
        operationType
      });
      
      // Optimistically resolve the mutation
      // This allows the UI to update as if the mutation succeeded
      observer.next({
        data: createOptimisticResponse(operation),
        extensions: {
          isOffline: true,
          queued: true,
          timestamp: Date.now()
        }
      });
      
      observer.complete();
    });
  }
  
  // For queries, let them pass through but they'll likely error
  // We could handle this differently, like returning cached data only
  return forward(operation);
});

/**
 * Creates an optimistic response for a mutation
 * This is a simplified version - in a real app, you'd want to make this more robust
 */
function createOptimisticResponse(operation) {
  const operationType = operation.query.definitions[0]?.operation;
  const operationName = operation.operationName;
  
  // Basic optimistic response that mimics successful operation
  // In a real app, you'd use optimistic response capabilities more fully
  const response = {};
  
  // Get the mutation name (first field in the mutation selection)
  const selectionSet = operation.query.definitions[0]?.selectionSet;
  if (selectionSet && selectionSet.selections && selectionSet.selections.length > 0) {
    const mutationField = selectionSet.selections[0];
    const mutationName = mutationField.name.value;
    
    // Create a placeholder response
    response[mutationName] = {
      __typename: `${mutationName}Payload`,
      success: true,
      message: 'Operation queued for processing when online'
    };
    
    // If the mutation has a specific expected structure, you could add more specific fields here
    // This would be custom to your schema
  }
  
  return response;
} 