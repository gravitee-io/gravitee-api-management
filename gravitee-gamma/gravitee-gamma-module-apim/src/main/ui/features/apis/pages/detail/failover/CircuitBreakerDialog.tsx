/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@gravitee/graphene-core';

import circuitBreakerImg from './circuit-breaker.png';

const DESCRIPTION =
    'Circuit breaker is a mechanism used to detect failures and encapsulates the logic of preventing a failure from constantly recurring. In the case of failover, circuit breaker records the slow calls to the server as failures. Once the failures reach a certain threshold, the circuit breaker trips and all further calls to the circuit breaker return with an error, without a new request to the remote server being made at all. After a certain amount of time in open state, the circuit breaker will try to reset by trying a call to see if the problem is fixed, this state is the half-open state.';

interface CircuitBreakerDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function CircuitBreakerDialog({ open, onOpenChange }: Readonly<CircuitBreakerDialogProps>) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ maxWidth: '680px' }}>
                <DialogHeader>
                    <DialogTitle>Failover</DialogTitle>
                </DialogHeader>
                <p className="text-sm leading-relaxed text-muted-foreground">{DESCRIPTION}</p>
                <div className="flex items-center justify-center rounded-lg bg-muted p-6">
                    <img src={circuitBreakerImg} alt="Circuit breaker state diagram" className="max-w-full" />
                </div>
            </DialogContent>
        </Dialog>
    );
}
