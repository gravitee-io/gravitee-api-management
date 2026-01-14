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

export interface TopicConfig {
  name: string;
  partitions: number;
  replicas: number;
  retentionMs?: number;
  cleanupPolicy?: string;
}

@Component({
  selector: 'add-topic-dialog',
  templateUrl: './add-topic-dialog.component.html',
  styleUrls: ['./add-topic-dialog.component.scss'],
  standalone: false,
})
export class AddTopicDialogComponent {
  topicConfig: TopicConfig = {
    name: '',
    partitions: 3,
    replicas: 1,
    retentionMs: 604800000, // 7 days
    cleanupPolicy: 'delete',
  };

  cleanupPolicies = ['delete', 'compact', 'compact,delete'];

  constructor(
    public dialogRef: MatDialogRef<AddTopicDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {}

  get isFormValid(): boolean {
    return !!(
      this.topicConfig.name &&
      this.topicConfig.name.match(/^[a-zA-Z0-9._-]+$/) &&
      this.topicConfig.partitions > 0 &&
      this.topicConfig.replicas > 0
    );
  }

  get retentionDays(): number {
    return Math.floor((this.topicConfig.retentionMs || 0) / (1000 * 60 * 60 * 24));
  }

  set retentionDays(days: number) {
    this.topicConfig.retentionMs = days * 24 * 60 * 60 * 1000;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onCreate(): void {
    if (this.isFormValid) {
      this.dialogRef.close(this.topicConfig);
    }
  }
}
