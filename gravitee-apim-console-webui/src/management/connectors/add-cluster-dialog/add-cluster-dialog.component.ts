/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { KafkaManagementService } from '../services/kafka-management.service';

export interface ClusterConfig {
  name: string;
  bootstrapServers: string;
  securityProtocol: string;
  saslMechanism?: string;
  saslUsername?: string;
  saslPassword?: string;
}

@Component({
  selector: 'add-cluster-dialog',
  templateUrl: './add-cluster-dialog.component.html',
  styleUrls: ['./add-cluster-dialog.component.scss'],
  standalone: false,
})
export class AddClusterDialogComponent {
  clusterConfig: ClusterConfig = {
    name: '',
    bootstrapServers: 'localhost:9091',
    securityProtocol: 'PLAINTEXT',
  };

  securityProtocols = ['PLAINTEXT', 'SASL_PLAINTEXT', 'SASL_SSL', 'SSL'];
  saslMechanisms = ['PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512', 'GSSAPI'];

  isTestingConnection = false;
  connectionTestResult: { success: boolean; message: string } | null = null;

  constructor(
    public dialogRef: MatDialogRef<AddClusterDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private kafkaManagementService: KafkaManagementService,
  ) {}

  get isSaslEnabled(): boolean {
    return this.clusterConfig.securityProtocol.includes('SASL');
  }

  get isFormValid(): boolean {
    return !!(
      this.clusterConfig.name &&
      this.clusterConfig.bootstrapServers &&
      (!this.isSaslEnabled || (this.clusterConfig.saslUsername && this.clusterConfig.saslPassword))
    );
  }

  async testConnection() {
    this.isTestingConnection = true;
    this.connectionTestResult = null;

    // Call real backend API to test Kafka connection
    this.kafkaManagementService
      .testConnection({
        bootstrapServers: this.clusterConfig.bootstrapServers,
        securityProtocol: this.clusterConfig.securityProtocol,
        saslMechanism: this.clusterConfig.saslMechanism,
        saslUsername: this.clusterConfig.saslUsername,
        saslPassword: this.clusterConfig.saslPassword,
      })
      .subscribe({
        next: (response) => {
          this.connectionTestResult = {
            success: response.success,
            message: response.message,
          };
          this.isTestingConnection = false;
        },
        error: (error) => {
          this.connectionTestResult = {
            success: false,
            message: `Failed to connect: ${error.error?.error || error.message || 'Unknown error'}`,
          };
          this.isTestingConnection = false;
        },
      });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.isFormValid) {
      this.dialogRef.close(this.clusterConfig);
    }
  }
}
