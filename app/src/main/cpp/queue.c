/*Queue - Linked List implementation*/
#include<stdio.h>
#include<stdlib.h>
struct Node {
	int data;
	struct Node* next;
};
// Two glboal variables to store address of front and rear nodes. 
struct Node* front = NULL;
struct Node* rear = NULL;
int size = 0;

void InitQueue(int max){}
// To Enqueue an integer
int Enqueue(int x) { //return size
	struct Node* temp = 
		(struct Node*)malloc(sizeof(struct Node));
	temp->data =x; 
	temp->next = NULL;
	if(front == NULL && rear == NULL){
		front = rear = temp;
		return;
	}
	rear->next = temp;
	rear = temp;
	size ++;
	return size;
}

// To Dequeue an integer.
void Pop() {
	struct Node* temp = front;
	if(front == NULL) {
		printf("Queue is Empty\n");
		return;
	}
	if(front == rear) {
		front = rear = NULL;
	}
	else {
		front = front->next;
	}
	free(temp);
    size --;
}

int Dequeue() {
	if(front == NULL) {
		printf("Queue is empty\n");
		return;
	}
    int result = front->data;
    Pop();
	return result;
}
int isEmpty(){
    if(front == NULL)
        return 1;
    else
        return 0;
}

void Print() {
	struct Node* temp = front;
	while(temp != NULL) {
		printf("%d ",temp->data);
		temp = temp->next;
	}
	printf("\n");
}

int main(){
	/* Drive code to test the implementation. */
	// Printing elements in Queue after each Enqueue or Dequeue 
	Enqueue(2); Print(); 
	Enqueue(4); Print();
	Enqueue(6); Print();
	Dequeue();  Print();
	Enqueue(8); Print();
}

int release(){
}


