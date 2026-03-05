/* Auto-generated from CircleCI schema.json — do not edit */
// @ts-nocheck

export type SelfHostedRunner = string;
export type Environment =
  | (string | null)
  | Environment[]
  | {
      [k: string]: string | boolean | number | null;
    };
export type Step =
  | string
  | {
      run?:
        | string
        | {
            command: string;
            name?: string;
            shell?: string;
            environment?: Environment1;
            background?: boolean;
            working_directory?: string;
            no_output_timeout?: string;
            when?: "always" | "on_success" | "on_fail";
            max_auto_reruns?: number;
            auto_rerun_delay?: string;
            [k: string]: unknown;
          };
      checkout?:
        | "checkout"
        | {
            path?: string;
            method?: "blobless" | "full" | "shallow";
            depth?: number;
          };
      setup_remote_docker?:
        | "setup_remote_docker"
        | {
            docker_layer_caching?: boolean;
            version?:
              | (
                  | "20.10.24"
                  | "20.10.23"
                  | "20.10.18"
                  | "20.10.17"
                  | "20.10.14"
                  | "20.10.12"
                  | "20.10.11"
                  | "20.10.7"
                  | "20.10.6"
                  | "20.10.2"
                  | "19.03.13"
                )
              | string;
          };
      save_cache?: {
        paths: string[];
        key: string;
        name?: string;
        when?: "always" | "on_success" | "on_fail";
      };
      restore_cache?:
        | {
            key: string;
            name?: string;
          }
        | {
            keys: string[];
            name?: string;
          };
      store_artifacts?: {
        path: string;
        destination?: string;
        name?: string;
      };
      store_test_results?: {
        path: string;
        name?: string;
      };
      persist_to_workspace?: {
        root: string;
        paths: string[];
        name?: string;
      };
      attach_workspace?: {
        at: string;
        name?: string;
      };
      add_ssh_keys?:
        | "add_ssh_keys"
        | {
            fingerprints?: string[];
          };
      [k: string]:
        | {
            [k: string]: unknown;
          }
        | string;
    }
  | {
      when?: {
        condition: Logic;
        steps: Step[] | Step;
        [k: string]: unknown;
      };
      unless?: {
        condition: Logic3;
        steps: Step[] | Step;
        [k: string]: unknown;
      };
    };
export type Environment1 =
  | (string | null)
  | Environment[]
  | {
      [k: string]: string | boolean | number | null;
    };
export type Logic =
  | (string | boolean | number)
  | {
      /**
       * @minItems 1
       */
      and?: [Logic1, ...Logic1[]];
      /**
       * @minItems 1
       */
      or?: [Logic1, ...Logic1[]];
      not?: Logic2;
      /**
       * @minItems 1
       */
      equal?: [Logic1, ...Logic1[]];
      matches?: {
        pattern: string;
        value: string;
      };
    }
  | {
      [k: string]: unknown;
    };
export type Logic1 =
  | (string | boolean | number)
  | {
      /**
       * @minItems 1
       */
      and?: [Logic1, ...Logic1[]];
      /**
       * @minItems 1
       */
      or?: [Logic1, ...Logic1[]];
      not?: Logic2;
      /**
       * @minItems 1
       */
      equal?: [Logic1, ...Logic1[]];
      matches?: {
        pattern: string;
        value: string;
      };
    }
  | {
      [k: string]: unknown;
    };
export type Logic2 =
  | (string | boolean | number)
  | {
      /**
       * @minItems 1
       */
      and?: [Logic1, ...Logic1[]];
      /**
       * @minItems 1
       */
      or?: [Logic1, ...Logic1[]];
      not?: Logic2;
      /**
       * @minItems 1
       */
      equal?: [Logic1, ...Logic1[]];
      matches?: {
        pattern: string;
        value: string;
      };
    }
  | {
      [k: string]: unknown;
    };
export type Logic3 =
  | (string | boolean | number)
  | {
      /**
       * @minItems 1
       */
      and?: [Logic1, ...Logic1[]];
      /**
       * @minItems 1
       */
      or?: [Logic1, ...Logic1[]];
      not?: Logic2;
      /**
       * @minItems 1
       */
      equal?: [Logic1, ...Logic1[]];
      matches?: {
        pattern: string;
        value: string;
      };
    }
  | {
      [k: string]: unknown;
    };
export type Logic4 =
  | (string | boolean | number)
  | {
      /**
       * @minItems 1
       */
      and?: [Logic1, ...Logic1[]];
      /**
       * @minItems 1
       */
      or?: [Logic1, ...Logic1[]];
      not?: Logic2;
      /**
       * @minItems 1
       */
      equal?: [Logic1, ...Logic1[]];
      matches?: {
        pattern: string;
        value: string;
      };
    }
  | {
      [k: string]: unknown;
    };
export type Logic5 =
  | (string | boolean | number)
  | {
      /**
       * @minItems 1
       */
      and?: [Logic1, ...Logic1[]];
      /**
       * @minItems 1
       */
      or?: [Logic1, ...Logic1[]];
      not?: Logic2;
      /**
       * @minItems 1
       */
      equal?: [Logic1, ...Logic1[]];
      matches?: {
        pattern: string;
        value: string;
      };
    }
  | {
      [k: string]: unknown;
    };
export type JobDefinition =
  | string
  | {
      type?: "build" | "release" | "lock" | "unlock" | "approval" | "no-op";
      [k: string]: unknown;
    };
export type Orbs = {
  [k: string]:
    | string
    | {
        jobs?: {
          [k: string]: JobDefinition;
        };
        commands?: {
          [k: string]:
            | {
                description?: string;
                parameters?: {
                  [k: string]: {
                    type: "boolean" | "string" | "steps" | "enum" | "executor" | "integer" | "env_var_name";
                    default?: string | boolean | number | Step[];
                    description?: string;
                    enum?: string[];
                  };
                };
                /**
                 * @minItems 1
                 */
                steps: [Step, ...Step[]];
              }
            | string;
        };
        executors?: {
          [k: string]:
            | {
                description?: string;
                macos?: {
                  xcode: string | number;
                  resource_class?: string;
                  shell?: string;
                };
                resource_class?: string;
                /**
                 * @minItems 1
                 */
                docker?: [
                  {
                    image: string;
                    name?: string;
                    entrypoint?: string | string[];
                    command?: string | string[];
                    user?: string;
                    environment?:
                      | (string | null)
                      | Environment[]
                      | {
                          [k: string]: string | boolean | number | null;
                        };
                    aws_auth?:
                      | {
                          aws_access_key_id: string;
                          aws_secret_access_key: string;
                        }
                      | {
                          oidc_role_arn: string;
                        };
                    auth?: {
                      username: string;
                      password: string;
                    };
                  },
                  ...{
                    image: string;
                    name?: string;
                    entrypoint?: string | string[];
                    command?: string | string[];
                    user?: string;
                    environment?:
                      | (string | null)
                      | Environment[]
                      | {
                          [k: string]: string | boolean | number | null;
                        };
                    aws_auth?:
                      | {
                          aws_access_key_id: string;
                          aws_secret_access_key: string;
                        }
                      | {
                          oidc_role_arn: string;
                        };
                    auth?: {
                      username: string;
                      password: string;
                    };
                  }[]
                ];
                working_directory?: string;
                machine?:
                  | (string | boolean | number)
                  | {
                      enabled?: string | boolean | number;
                      image?: string;
                      docker_layer_caching?: string | boolean | number;
                      resource_class?: string;
                      shell?: string;
                    };
                environment?:
                  | (string | null)
                  | Environment[]
                  | {
                      [k: string]: string | boolean | number | null;
                    };
                shell?: string | string[];
                parameters?: {
                  [k: string]: {
                    type: "boolean" | "string" | "steps" | "enum" | "executor" | "integer" | "env_var_name";
                    default?: string | boolean | number | Step[];
                    description?: string;
                    enum?: string[];
                  };
                };
              }
            | string;
        };
        orbs?: Orbs;
        [k: string]: unknown;
      };
} | null;

export interface CircleCIConfig {
  executors?: {
    [k: string]:
      | {
          description?: string;
          macos?: {
            xcode: string | number;
            resource_class?: string;
            shell?: string;
          };
          resource_class?:
            | (
                | "small"
                | "medium"
                | "medium+"
                | "large"
                | "xlarge"
                | "2xlarge"
                | "2xlarge+"
                | "arm.medium"
                | "arm.large"
                | "arm.xlarge"
                | "arm.2xlarge"
                | "gpu.nvidia.small"
                | "gpu.nvidia.medium"
                | "windows.gpu.nvidia.medium"
                | "m4pro.medium"
                | "m4pro.large"
                | "medium-gen2"
                | "large-gen2"
                | "xlarge-gen2"
                | "2xlarge-gen2"
                | "2xlarge+-gen2"
              )
            | SelfHostedRunner;
          /**
           * @minItems 1
           */
          docker?: [
            {
              image: string;
              name?: string;
              entrypoint?: string | string[];
              command?: string | string[];
              user?: string;
              environment?:
                | (string | null)
                | Environment[]
                | {
                    [k: string]: string | boolean | number | null;
                  };
              aws_auth?:
                | {
                    aws_access_key_id: string;
                    aws_secret_access_key: string;
                  }
                | {
                    oidc_role_arn: string;
                  };
              auth?: {
                username: string;
                password: string;
              };
            },
            ...{
              image: string;
              name?: string;
              entrypoint?: string | string[];
              command?: string | string[];
              user?: string;
              environment?:
                | (string | null)
                | Environment[]
                | {
                    [k: string]: string | boolean | number | null;
                  };
              aws_auth?:
                | {
                    aws_access_key_id: string;
                    aws_secret_access_key: string;
                  }
                | {
                    oidc_role_arn: string;
                  };
              auth?: {
                username: string;
                password: string;
              };
            }[]
          ];
          working_directory?: string;
          machine?:
            | (string | boolean | number)
            | {
                enabled?: string | boolean | number;
                image?: string;
                docker_layer_caching?: string | boolean | number;
                resource_class?: string;
                shell?: string;
              };
          environment?:
            | (string | null)
            | Environment[]
            | {
                [k: string]: string | boolean | number | null;
              };
          shell?: string | string[];
          parameters?: {
            [k: string]: {
              type: "boolean" | "string" | "steps" | "enum" | "executor" | "integer" | "env_var_name";
              default?: string | boolean | number | Step[];
              description?: string;
              enum?: string[];
            };
          };
        }
      | string;
  } | null;
  experimental?: {
    notify: {
      branches: {
        only?: string | string[];
        ignore?: string | string[];
      };
    };
  };
  workflows?: {
    [k: string]: {
      triggers?: {
        schedule?: {
          cron?: string;
          filters?: {
            branches?: {
              only?: string | string[];
              ignore?: string | string[];
            };
          };
          [k: string]: unknown;
        };
      }[];
      max_auto_reruns?: number;
      when?: Logic4;
      unless?: Logic5;
      /**
       * @minItems 1
       */
      jobs: [
        (
          | string
          | {
              [k: string]: {
                requires?: (
                  | string
                  | {
                      /**
                       * This interface was referenced by `undefined`'s JSON-Schema definition
                       * via the `patternProperty` "^[A-Za-z][A-Za-z\s\d_-]*$".
                       */
                      [k: string]:
                        | ("success" | "failed" | "canceled" | "not_run" | "terminal")
                        | ("success" | "failed" | "canceled" | "not_run")[];
                    }
                )[];
                filters?:
                  | {
                      branches?: {
                        only?: string | string[];
                        ignore?: string | string[];
                      };
                      tags?: {
                        only?: string | string[];
                        ignore?: string | string[];
                      };
                      [k: string]: unknown;
                    }
                  | string;
                context?: string | string[];
                type?: string;
                "pre-steps"?: {
                  [k: string]: unknown;
                };
                "post-steps"?: {
                  [k: string]: unknown;
                };
                matrix?: {
                  parameters: {
                    [k: string]: unknown;
                  };
                  exclude?: {
                    [k: string]: unknown;
                  }[];
                  alias?: string;
                  [k: string]: unknown;
                };
                "serial-group"?: string;
                "override-with"?: string;
                [k: string]: unknown;
              };
            }
        ),
        ...(
          | string
          | {
              [k: string]: {
                requires?: (
                  | string
                  | {
                      /**
                       * This interface was referenced by `undefined`'s JSON-Schema definition
                       * via the `patternProperty` "^[A-Za-z][A-Za-z\s\d_-]*$".
                       */
                      [k: string]:
                        | ("success" | "failed" | "canceled" | "not_run" | "terminal")
                        | ("success" | "failed" | "canceled" | "not_run")[];
                    }
                )[];
                filters?:
                  | {
                      branches?: {
                        only?: string | string[];
                        ignore?: string | string[];
                      };
                      tags?: {
                        only?: string | string[];
                        ignore?: string | string[];
                      };
                      [k: string]: unknown;
                    }
                  | string;
                context?: string | string[];
                type?: string;
                "pre-steps"?: {
                  [k: string]: unknown;
                };
                "post-steps"?: {
                  [k: string]: unknown;
                };
                matrix?: {
                  parameters: {
                    [k: string]: unknown;
                  };
                  exclude?: {
                    [k: string]: unknown;
                  }[];
                  alias?: string;
                  [k: string]: unknown;
                };
                "serial-group"?: string;
                "override-with"?: string;
                [k: string]: unknown;
              };
            }
        )[]
      ];
      [k: string]: unknown;
    };
  };
  jobs?: {
    [k: string]: JobDefinition;
  };
  orbs?: Orbs;
  commands?: {
    [k: string]:
      | {
          description?: string;
          parameters?: {
            [k: string]: {
              type: "boolean" | "string" | "steps" | "enum" | "executor" | "integer" | "env_var_name";
              default?: string | boolean | number | Step[];
              description?: string;
              enum?: string[];
            };
          };
          /**
           * @minItems 1
           */
          steps: [Step, ...Step[]];
        }
      | string;
  } | null;
  examples?: {
    [k: string]: {
      description?: string;
      usage: {
        [k: string]: unknown;
      };
      result?: {
        [k: string]: unknown;
      };
    };
  };
  display?: {
    home_url?: string;
    source_url?: string;
    [k: string]: unknown;
  };
  version: "2.1" | 2.1;
  parameters?: {
    [k: string]: {
      type: "boolean" | "string" | "enum" | "integer";
      default: string | boolean | number;
      description?: string;
      enum?: string[];
    };
  } | null;
  [k: string]: unknown;
}
