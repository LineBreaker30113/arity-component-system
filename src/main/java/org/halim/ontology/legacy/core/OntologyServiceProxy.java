package org.halim.ontology.legacy.core;


import org.halim.ontology.legacy.core.segmentation.helper.OntologyTransaction;
import org.halim.ontology.legacy.core.segmentation.helper.BypassCrossCuttingConcerns;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OntologyServiceProxy implements InvocationHandler {

private final Object coreDomainImplementation;
private final ReentrantReadWriteLock globalLock;
private final UndoRedoStack undoRedoStack;
private final OntologyServiceLogger logger;
private final String serviceInterfaceName;

public OntologyServiceProxy(Object coreDomainImplementation, ReentrantReadWriteLock globalLock,
	  UndoRedoStack undoRedoStack, OntologyServiceLogger logger, String serviceInterfaceName) {
	this.coreDomainImplementation = coreDomainImplementation;
	this.globalLock = globalLock;
	this.undoRedoStack = undoRedoStack;
	this.logger = logger;
	this.serviceInterfaceName = serviceInterfaceName;
}

@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	// NUANCE: Do not proxy basic Object methods (toString, hashCode, equals).
	// Let them execute directly on the underlying implementation without locking.
	if (method.getDeclaringClass() == Object.class) {
		return method.invoke(coreDomainImplementation, args);
	}

	OntologyTransaction command = method.getAnnotation(OntologyTransaction.class);
	// 1. Look directly on the method first
	BypassCrossCuttingConcerns bypassMeta = method.getAnnotation(BypassCrossCuttingConcerns.class);

	// 2. If not found, check if any of the method's annotations are themselves annotated with it (Meta-Annotation)
	if (bypassMeta == null) {
		for (java.lang.annotation.Annotation ann : method.getAnnotations()) {
			if (ann.annotationType().isAnnotationPresent(BypassCrossCuttingConcerns.class)) {
				bypassMeta = ann.annotationType().getAnnotation(BypassCrossCuttingConcerns.class);
				break;
			}
		}
	}
	List<BypassCrossCuttingConcerns.CrossCuttingConcern> bypassed =
		bypassMeta != null ? Arrays.asList(bypassMeta.value()) : List.of();

	boolean requireWriteLock = !bypassed.contains(BypassCrossCuttingConcerns.CrossCuttingConcern.LOCKING_W)
		&& !bypassed.contains(BypassCrossCuttingConcerns.CrossCuttingConcern.ALL);
	boolean requireReadLock = !requireWriteLock
		&& !bypassed.contains(BypassCrossCuttingConcerns.CrossCuttingConcern.LOCKING_R)
		&& !bypassed.contains(BypassCrossCuttingConcerns.CrossCuttingConcern.ALL);
	boolean requireUndo = command != null
		&& !bypassed.contains(BypassCrossCuttingConcerns.CrossCuttingConcern.UNDO_REDO)
		&& !bypassed.contains(BypassCrossCuttingConcerns.CrossCuttingConcern.ALL);
	boolean requireLogging = !bypassed.contains(BypassCrossCuttingConcerns.CrossCuttingConcern.LOGGING)
		&& !bypassed.contains(BypassCrossCuttingConcerns.CrossCuttingConcern.ALL);

	if (requireWriteLock) {
		globalLock.writeLock().lock();
	} else if (requireReadLock) {
		globalLock.readLock().lock();
	}

	if (requireUndo) {
		TransactionContext.begin(command.action());
	}

	long startNanos = requireLogging ? System.nanoTime() : 0L;
	if (requireLogging) logger.onInvoke(serviceInterfaceName, method.getName(), args);

	try {
		Object result = method.invoke(coreDomainImplementation, args);
		if (requireUndo) {
			TransactionContext.Transaction committed = TransactionContext.commit();
			if (committed != null) undoRedoStack.pushCommitted(committed);
		}
		if (requireLogging) logger.onSuccess(serviceInterfaceName, method.getName(), result, System.nanoTime() - startNanos);
		return result;
	} catch (Exception e) {
		if (requireUndo) TransactionContext.rollback();
		if (requireLogging) logger.onFailure(serviceInterfaceName, method.getName(), e, System.nanoTime() - startNanos);
		throw e;
	} finally {
		if (requireWriteLock) {
			globalLock.writeLock().unlock();
		} else if (requireReadLock) {
			globalLock.readLock().unlock();
		}
	}
}
}
